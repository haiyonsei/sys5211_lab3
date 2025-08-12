
package npu

import chisel3.stage.ChiselStage

/**
  * Simple entry point to emit Verilog for SimpleReservationStation.
  *
  * Usage examples (SBT):
  *   sbt "runMain npu.EmitVerilog"
  *   sbt -DN_LD=4 -DN_EX=2 -DN_ST=2 -DADDR_W=16 -DtargetDir=generated "runMain npu.EmitVerilog"
  *
  * You can override queue sizes and address width via JVM system properties:
  *   -DN_LD, -DN_EX, -DN_ST, -DADDR_W, -DtargetDir
  *
  * If your module is in a different package (e.g., gemmini), change the package line accordingly
  * and the runMain target (e.g., gemmini.EmitVerilog).
  */
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
