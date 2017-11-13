package scoobie.dsl.schemaless.ansi.sql

import scoobie.ast.ansi._
import scoobie.ast.coercion.Coerce

/**
  * Created by jacob.barber on 5/24/16.
  */
trait primitives {
  class QueryValueExtensions[F[_]](val a: QueryValue[F]) {
    def >(b: QueryValue[F]): QueryComparison[F] = QueryGreaterThan(a, b)
    def >=(b: QueryValue[F]): QueryComparison[F] = QueryGreaterThanOrEqual(a, b)
    def <(b: QueryValue[F]): QueryComparison[F] = QueryLessThan(a, b)
    def <=(b: QueryValue[F]): QueryComparison[F] = QueryLessThanOrEqual(a, b)
    def ===(b: QueryValue[F]): QueryComparison[F] = QueryEqual(a, b)
    def !==(b: QueryValue[F]): QueryComparison[F] = QueryNot(QueryEqual(a, b))
    def <>(b: QueryValue[F]): QueryComparison[F] = !==(b)

    def +(b: QueryValue[F]): QueryValue[F] = QueryAdd(a, b)
    def -(b: QueryValue[F]): QueryValue[F] = QuerySub(a, b)
    def /(b: QueryValue[F]): QueryValue[F] = QueryDiv(a, b)
    def *(b: QueryValue[F]): QueryValue[F] = QueryMul(a, b)

    def in(values: QueryValue[F]*): QueryComparison[F] = QueryIn(a, values.toList)
    def notIn(values: QueryValue[F]*): QueryComparison[F] = QueryNot(QueryIn(a, values.toList))

    def as(alias: String): QueryProjection[F] = QueryProjectOne(a, Some(alias))
  }

  class SqlQueryFunctionBuilder[F[_]](val f: QueryPath[F]) {
    def apply(params: QueryValue[F]*): QueryFunction[F] = QueryFunction(f, params.toList)
  }

  class SqlDslStringInterpolators[F[_]](val ctx: StringContext)(implicit coerce: Coerce[F]) {
    def p(): QueryPath[F] = {
      def go(remainingParts: List[String], queryPath: QueryPath[F]): QueryPath[F] = remainingParts match {
        case head :: tail => go(tail, QueryPathCons(head, queryPath))
        case Nil => queryPath
      }

      val parts = ctx.parts.mkString.split('.').toList.reverse
      go(parts.tail, QueryPathEnd(parts.head))
    }

    def expr(args: String*): QueryRawExpression[F, String] = {
      QueryRawExpression(ctx.standardInterpolator(identity, args))
    }

    def func(): SqlQueryFunctionBuilder[F] = new SqlQueryFunctionBuilder(p())
  }

  class QueryProjectionExtensions[F[_]](val a: QueryProjection[F])(implicit coerce: Coerce[F]) {
    def as(alias: String): QueryProjection[F] = a match {
      case _: QueryProjectAll[F] => a: QueryProjection[F]
      case QueryProjectOne(selection, _) => QueryProjectOne(selection, Some(alias))
    }

    def on(comparison: QueryComparison[F]): (QueryProjection[F], QueryComparison[F]) = (a, comparison)
  }

  class QueryComparisonExtensions[F[_]](val left: QueryComparison[F]) {
    def and(right: QueryComparison[F]) = QueryAnd(left, right)
    def or(right: QueryComparison[F]) = QueryOr(left, right)
  }

}
