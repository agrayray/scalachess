package chess
package format.pgn

object Dumper {

  def apply(situation: Situation, data: chess.Move, next: Situation): String = {
    import data._

    val base = (promotion, piece.role) match {
      case _ if castles =>
        if (orig ?> dest) "O-O-O" else "O-O"

      case _ if enpassant =>
        orig.file.toString + "x" + dest.key

      case (promotion, Pawn) =>
        (if (captures) s"${orig.file}x" else "") +
          promotion.fold(dest.key)(p => s"${dest.key}=${p.pgn}")

      case (_, role) =>
        // Check whether there is a need to disambiguate:
        //   - can a piece of same role move to/capture on the same square?
        //   - if so, disambiguate, in order or preference, by:
        //       - file
        //       - rank
        //       - both (only happens w/ at least 3 pieces of the same role)
        val candidates = situation.board.pieces collect {
          case (cpos, cpiece) if cpiece == piece && cpos != orig && cpiece.eyes(cpos, dest) => cpos
        } filter { cpos =>
          // We know Role ≠ Pawn, so it is fine to always pass None as promotion target
          situation.move(cpos, dest, None).isValid
        }

        val disambiguation = if (candidates.isEmpty) {
          ""
        } else if (!candidates.exists(_ ?| orig)) {
          orig.file.toString
        } else if (!candidates.exists(_ ?- orig)) {
          orig.rank.toString
        } else {
          orig.key
        }
        s"${role.pgn}$disambiguation${if (captures) "x" else ""}${dest.key}"
    }

    s"$base${checkOrWinnerSymbol(next)}"
  }

  def apply(data: chess.Drop, next: Situation): String =
    s"${data.toUci.uci}${checkOrWinnerSymbol(next)}"

  private def checkOrWinnerSymbol(next: Situation): String =
    if (next.winner.isDefined) "#"
    else if (next.check) "+"
    else ""

  def apply(data: chess.Move): String =
    apply(
      data.situationBefore,
      data,
      data.finalizeAfter situationOf !data.color
    )

  def apply(data: chess.Drop): String =
    apply(
      data,
      data.finalizeAfter situationOf !data.color
    )
}
