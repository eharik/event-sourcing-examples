package eph.events

object Examples {

  // Stateful Sink

  def consumeEvent[F[_]: Effect, A: Deserializer](consumer: KafkaConsumer, offset: KafkaOffset): F[A]

  def persist[F[_]: Effect, A](a: A, repo: Repo[A]): F[A]

  def commitOffset[F[_]: Effect](consumer: KafkaConsumer, offset: KafkaOffset): F[Unit]

  def run[F[_]: Effect, A: Deserializer](
      consumer: KafkaConsumer,
      offset: KafkaOffset,
      repo: Repo[A]): F[Unit] =
    for {
      a <- consumeEvent(consumer, offset) // step 1
      _ <- persist(a, repo)               // step 2
      _ <- commitOffset(consumer, offset) // step 3
    } yield ()

  // Idempotent DB Actions

  trait IdempInsertResult[+A]
  case object DuplicateInsert extends IdempInsertResult[Nothing]
  case class Insert[A](a: A) extends IdempInsertResult[A]

  def idempInsert[F[_]: Effect, A](a: A, repo: Repo[A]): F[IdempInsert[A]]

  // Stateless Processing

  def processEvent[A, B](a: A): F[B]
  def publishEvent[F[_]: Effect, B: Serializer](publisher: KafkaPublisher, b: B): F[Unit]

  def run[
      F[_]: Effect,
      A: Deserializer,
      B: Serializer](
      consumer: KafkaConsumer,
      offset: KafkaOffset,
      publisher: KafkaPublisher): F[Unit] =
    for {
      a <- consumeEvent(consumer, offset)  // 1
      b <- processEvent(a)                 // 2
      _ <- publishEvent(publisher, b)      // 3
      _ <- commitOffset(consumer, offset)  // 4
    } yield ()

  // Stateful Processing

  def fetch[A](id: UUID, repo: Repo[A]): F[Option[A]]

  def run[
      F[_]: Effect,
      A: Deserializer,
      B: Serializer](
      consumer: KafkaConsumer,
      offset: KafkaOffset,
      publisher: KafkaPublisher,
      repo: Repo[A]): F[Unit] =
    for {
      a  <- consumeEvent(consumer, offset) // 1
      id <- persist(a, repo)               // 2
      b  <- fetch(id, repo)                // 3
      _  <- publishEvent(b)                // 4
      _  <- commitOffset(consumer, offset) // 5
    } yield ()
}
