
package npu

import chisel3.stage.ChiselStage

object EmitVerilog extends App {
  val nLd   = sys.props.get("N_LD").map(_.toInt).getOrElse(2)
  val nEx   = sys.props.get("N_EX").map(_.toInt).getOrElse(2)
  val nSt   = sys.props.get("N_ST").map(_.toInt).getOrElse(2)
  val addrW = sys.props.get("ADDR_W").map(_.toInt).getOrElse(16)

  (new ChiselStage).emitVerilog(
    new SimpleReservationStation(nLd, nEx, nSt, addrW),
    Array("--target-dir", "build")
  )
}
