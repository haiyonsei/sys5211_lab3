package npu


// =====================================================================
// ========================== TESTS BELOW ===============================
// =====================================================================

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


object SimpleRS {
  val Q_LD = 0.U(2.W)
  val Q_EX = 1.U(2.W)
  val Q_ST = 2.U(2.W)
}



class SimpleReservationStationSpec extends AnyFlatSpec with ChiselScalatestTester {
  import SimpleRS._

  private def pokeCmd(port: DecoupledIO[SimpleRSCmd], q: UInt,
                      opaValid: Boolean, aStart: Int, aLen: Int,
                      opbValid: Boolean = false, bStart: Int = 0, bLen: Int = 0,
                      opaIsDst: Boolean = false): Unit = {
    port.bits.qType.poke(q)
    port.bits.opa.valid.poke(opaValid.B)
    port.bits.opa.start.poke(aStart.U)
    port.bits.opa.len.poke(aLen.U)
    port.bits.opb.valid.poke(opbValid.B)
    port.bits.opb.start.poke(bStart.U)
    port.bits.opb.len.poke(bLen.U)
    port.bits.opaIsDst.poke(opaIsDst.B)
  }

  behavior of "SimpleReservationStation (hazards + issue)"

  it should "block EX (RAW) until LD completes, then allow EX to issue" in {
    test(new SimpleReservationStation(2, 2, 2, 16)) { dut =>
      dut.io.issue.ld.ready.poke(true.B)
      dut.io.issue.ex.ready.poke(true.B)
      dut.io.issue.st.ready.poke(true.B)
      dut.io.completed.valid.poke(false.B)

      pokeCmd(dut.io.alloc, Q_LD, opaValid = true, aStart = 0, aLen = 8)
      dut.io.alloc.valid.poke(true.B); dut.clock.step(); dut.io.alloc.valid.poke(false.B)

      var ldRob: Option[BigInt] = None
      var i = 0
      while (i < 8 && ldRob.isEmpty) {
        if (dut.io.issue.ld.valid.peek().litToBoolean) {
          ldRob = Some(dut.io.issue.ld.robId.peek().litValue)
        }
        dut.clock.step(); i += 1
      }
      assert(ldRob.nonEmpty, "LD did not issue")

      pokeCmd(dut.io.alloc, Q_EX, opaValid = true, aStart = 0, aLen = 8)
      dut.io.alloc.valid.poke(true.B); dut.clock.step(); dut.io.alloc.valid.poke(false.B)

      for (_ <- 0 until 4) { dut.io.issue.ex.valid.expect(false.B); dut.clock.step() }

      dut.io.completed.valid.poke(true.B)
      dut.io.completed.bits.poke(ldRob.get.U)
      dut.clock.step()
      dut.io.completed.valid.poke(false.B)

      var exIssued = false
      for (_ <- 0 until 6) { if (dut.io.issue.ex.valid.peek().litToBoolean) exIssued = true; dut.clock.step() }
      assert(exIssued, "EX should issue after LD completion")
    }
  }

  it should "enforce in-order ISSUE within LD queue (second LD waits until first LD issues)" in {
    test(new SimpleReservationStation(2, 2, 2, 16)) { dut =>
      dut.io.issue.ld.ready.poke(true.B)
      dut.io.issue.ex.ready.poke(true.B)
      dut.io.issue.st.ready.poke(true.B)

      pokeCmd(dut.io.alloc, Q_LD, opaValid = true, aStart = 0, aLen = 4)
      dut.io.alloc.valid.poke(true.B); dut.clock.step(); dut.io.alloc.valid.poke(false.B)

      pokeCmd(dut.io.alloc, Q_LD, opaValid = true, aStart = 16, aLen = 4)
      dut.io.alloc.valid.poke(true.B); dut.clock.step(); dut.io.alloc.valid.poke(false.B)

      dut.io.issue.ld.valid.expect(true.B)
      val ld0Rob = dut.io.issue.ld.robId.peek().litValue
      dut.clock.step()

      dut.io.issue.ld.valid.expect(true.B)
      val ld1Rob = dut.io.issue.ld.robId.peek().litValue
      assert(ld1Rob != ld0Rob, "Second LD should have a different ROB id")
    }
  }

  it should "block ST (WAR vs EX-preload) until EX completes" in {
    test(new SimpleReservationStation(2, 2, 2, 16)) { dut =>
      dut.io.issue.ld.ready.poke(true.B)
      dut.io.issue.ex.ready.poke(true.B)
      dut.io.issue.st.ready.poke(true.B)

      pokeCmd(dut.io.alloc, Q_EX, opaValid = true, aStart = 32, aLen = 8, opaIsDst = true)
      dut.io.alloc.valid.poke(true.B); dut.clock.step(); dut.io.alloc.valid.poke(false.B)

      pokeCmd(dut.io.alloc, Q_ST, opaValid = true, aStart = 32, aLen = 8)
      dut.io.alloc.valid.poke(true.B); dut.clock.step(); dut.io.alloc.valid.poke(false.B)

      dut.io.issue.ex.valid.expect(true.B)
      val exRob = dut.io.issue.ex.robId.peek().litValue
      dut.clock.step()

      for (_ <- 0 until 3) { dut.io.issue.st.valid.expect(false.B); dut.clock.step() }

      dut.io.completed.valid.poke(true.B)
      dut.io.completed.bits.poke(exRob.U)
      dut.clock.step()
      dut.io.completed.valid.poke(false.B)

      var stIssued = false
      for (_ <- 0 until 6) { if (dut.io.issue.st.valid.peek().litToBoolean) stIssued = true; dut.clock.step() }
      assert(stIssued, "STORE should issue after EX-preload completion (WAR cleared)")
    }
  }
}

