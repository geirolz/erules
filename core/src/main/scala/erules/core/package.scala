package erules

import cats.Id
import cats.effect.IO

package object core {

  type AnyF[_]            = Any
  type EitherThrow[+T]    = Either[Throwable, T]
  type PureRule[-T]       = Rule[Id, T]
  type RuleIO[-T]         = Rule[IO, T]
  type PureRulesEngine[T] = RulesEngine[Id, T]
  type RulesEngineIO[T]   = RulesEngine[IO, T]

}
