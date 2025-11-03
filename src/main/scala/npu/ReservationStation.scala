package npu

import chisel3._
import chisel3.util._

/**
  * Reservation Station capturing the core ideas:
  *  - Three queues (LD / EX / ST) with small fixed depths
  *  - Interval-based hazard detection (RAW/WAR/WAW via overlap)
  *  - Same-queue deps cleared on ISSUE (in-order issue)
  *  - Cross-queue deps cleared on COMPLETION
  *  - One issue per queue per cycle (priority = lowest index)
  *  - Simple ROB id encoding: { qType[1:0], index[log2(Nmax)-1:0] }
  */

// ----------------------------
// Public command format
// ----------------------------
object SimpleRS {
  val Q_LD = 0.U(2.W)
  val Q_EX = 1.U(2.W)
  val Q_ST = 2.U(2.W)
}

class SimpleRSInterval(addrW: Int) extends Bundle {
  val valid = Bool()
  val start = UInt(addrW.W)
  val len   = UInt(addrW.W)
  def end: UInt = start + len
}

class SimpleRSCmd(addrW: Int) extends Bundle {
  val qType     = UInt(2.W)              // 0=LD, 1=EX, 2=ST
  val opa       = new SimpleRSInterval(addrW)
  val opb       = new SimpleRSInterval(addrW)
  val opaIsDst  = Bool()
}

class ReservationStationIssue[T <: Data](cmd_t: T, idWidth: Int) extends Bundle {
  val valid = Output(Bool())
  val ready = Input(Bool())
  val cmd   = Output(cmd_t.cloneType)
  val robId = Output(UInt(idWidth.W))
  def fire(dummy: Int = 0): Bool = valid && ready
}

class SimpleReservationStation(
  val N_LD: Int = 2,
  val N_EX: Int = 2,
  val N_ST: Int = 2,
  val ADDR_W: Int = 16
) extends Module {
  import SimpleRS._

  require(N_LD > 0 && N_EX > 0 && N_ST > 0)
  require(isPow2(N_LD) && isPow2(N_EX) && isPow2(N_ST), "Queue sizes must be powers of two for simple ROB encoding")

  private val N_MAX = Seq(N_LD, N_EX, N_ST).max
  private val IDX_W = log2Ceil(N_MAX)
  val ROB_ID_WIDTH  = 2 + IDX_W

  val io = IO(new Bundle {
    val alloc     = Flipped(Decoupled(new SimpleRSCmd(ADDR_W)))
    val completed = Flipped(Valid(UInt(ROB_ID_WIDTH.W))) // {qType, index}

    val issue = new Bundle {
      val ld = new ReservationStationIssue(new SimpleRSCmd(ADDR_W), ROB_ID_WIDTH)
      val ex = new ReservationStationIssue(new SimpleRSCmd(ADDR_W), ROB_ID_WIDTH)
      val st = new ReservationStationIssue(new SimpleRSCmd(ADDR_W), ROB_ID_WIDTH)
    }

    val busy = Output(Bool())
  })

  // ----------------------------
  // Entry & overlap
  // ----------------------------
  class Entry extends Bundle {
    val qType    = UInt(2.W)
    val opa      = new SimpleRSInterval(ADDR_W)
    val opb      = new SimpleRSInterval(ADDR_W)
    val opaIsDst = Bool() // only meaningful for EX (preload)

    val issued   = Bool()

    val deps_ld  = Vec(N_LD, Bool())
    val deps_ex  = Vec(N_EX, Bool())
    val deps_st  = Vec(N_ST, Bool())

    def ready: Bool = !(deps_ld.reduce(_||_) || deps_ex.reduce(_||_) || deps_st.reduce(_||_))
  }

  def overlaps(a: SimpleRSInterval, b: SimpleRSInterval): Bool = {
    a.valid && b.valid && (a.start < b.end) && (b.start < a.end)
  }

  // ----------------------------
  // Queues (arrays of entries)
  // ----------------------------
  val ldQ = RegInit(VecInit(Seq.fill(N_LD)(0.U.asTypeOf(Valid(new Entry)))))
  val exQ = RegInit(VecInit(Seq.fill(N_EX)(0.U.asTypeOf(Valid(new Entry)))))
  val stQ = RegInit(VecInit(Seq.fill(N_ST)(0.U.asTypeOf(Valid(new Entry)))))

  private def anyValid(v: Vec[Valid[Entry]]): Bool = v.map(_.valid).reduce(_||_)
  private def anyFree (v: Vec[Valid[Entry]]): Bool = v.map(e => !e.valid).reduce(_||_)

  io.busy := anyValid(ldQ) || anyValid(exQ) || anyValid(stQ)

  // ----------------------------
  // Allocate new entry
  // ----------------------------
  io.alloc.ready := false.B

  val newE = Wire(new Entry)
  newE := 0.U.asTypeOf(new Entry)
  when(io.alloc.valid) {
    newE.qType    := io.alloc.bits.qType
    newE.opa      := io.alloc.bits.opa
    newE.opb      := io.alloc.bits.opb
    newE.opaIsDst := io.alloc.bits.opaIsDst
    newE.issued   := false.B

    val isLD = io.alloc.bits.qType === Q_LD
    val isEX = io.alloc.bits.qType === Q_EX
    val isST = io.alloc.bits.qType === Q_ST

    // deps for LD
    newE.deps_ld := VecInit(ldQ.map(e => e.valid && !e.bits.issued)) // in-order LD queue
    // deps relations vs EX & ST
    val ld_vs_ex = exQ.map { e => e.valid && (overlaps(newE.opa, e.bits.opa) || overlaps(newE.opa, e.bits.opb)) }
    val ex_vs_ld = ldQ.map { e => e.valid && (overlaps(newE.opa, e.bits.opa) || overlaps(newE.opb, e.bits.opa)) }
    val st_vs_ex = exQ.map { e => e.valid && e.bits.opaIsDst && overlaps(newE.opa, e.bits.opa) }
    val ld_vs_st = stQ.map { e => e.valid && overlaps(newE.opa, e.bits.opa) }
    val st_vs_ld = ldQ.map { e => e.valid && overlaps(newE.opa, e.bits.opa) }
    val ex_vs_st = stQ.map { e => e.valid && newE.opaIsDst && overlaps(newE.opa, e.bits.opa) }

    when(isLD) {
      newE.deps_ex := VecInit(ld_vs_ex)
      newE.deps_st := VecInit(ld_vs_st)
    }.elsewhen(isEX) {
      newE.deps_ex := VecInit(exQ.map(e => e.valid && !e.bits.issued))
      newE.deps_st := VecInit(ex_vs_st)
      newE.deps_ld := VecInit(ex_vs_ld)
    }.otherwise {
      newE.deps_ex := VecInit(st_vs_ex)
      newE.deps_st := VecInit(stQ.map(e => e.valid && !e.bits.issued))
      newE.deps_ld := VecInit(st_vs_ld)
    }

    val ldFreeOH = VecInit(ldQ.map(e => !e.valid))
    val exFreeOH = VecInit(exQ.map(e => !e.valid))
    val stFreeOH = VecInit(stQ.map(e => !e.valid))

    val ldHasFree = anyFree(ldQ)
    val exHasFree = anyFree(exQ)
    val stHasFree = anyFree(stQ)

    io.alloc.ready := Mux1H(Seq(
      isLD -> ldHasFree,
      isEX -> exHasFree,
      isST -> stHasFree
    ))

    when(io.alloc.fire) {
      when(isLD) {
        val idx = PriorityEncoder(ldFreeOH)
        ldQ(idx).valid := true.B
        ldQ(idx).bits  := newE
      }.elsewhen(isEX) {
        val idx = PriorityEncoder(exFreeOH)
        exQ(idx).valid := true.B
        exQ(idx).bits  := newE
      }.otherwise {
        val idx = PriorityEncoder(stFreeOH)
        stQ(idx).valid := true.B
        stQ(idx).bits  := newE
      }
    }
  }

  // ----------------------------
  // Issue logic (specialized per queue)
  // ----------------------------
  private def issueLd(): Unit = {
    val canIssue = ldQ.map(e => e.valid && !e.bits.issued && e.bits.ready)
    val selOH    = PriorityEncoderOH(canIssue)
    val hasSel   = canIssue.reduce(_||_)
    val selIdx   = OHToUInt(selOH)

    val outCmd = Wire(new SimpleRSCmd(ADDR_W))
    outCmd := 0.U.asTypeOf(outCmd)
    when(hasSel) {
      val e = Mux1H(selOH, ldQ.map(_.bits))
      outCmd.qType    := e.qType
      outCmd.opa      := e.opa
      outCmd.opb      := e.opb
      outCmd.opaIsDst := e.opaIsDst
    }

    io.issue.ld.valid := hasSel && !io.alloc.valid
    io.issue.ld.cmd   := outCmd
    io.issue.ld.robId := Cat(Q_LD, selIdx.asUInt.pad(IDX_W))

    when(io.issue.ld.fire()) {
      for (i <- 0 until N_LD) {
        when(selOH(i)) { ldQ(i).bits.issued := true.B }
      }
      // clear deps_ld(i) across ALL entries
      for (i <- 0 until N_LD) {
        when(selOH(i)) {
          ldQ.foreach { e => e.bits.deps_ld(i) := false.B }
          //exQ.foreach { e => e.bits.deps_ld(i) := false.B }
          //stQ.foreach { e => e.bits.deps_ld(i) := false.B }
        }
      }
    }
  }

  private def issueEx(): Unit = {
    val canIssue = exQ.map(e => e.valid && !e.bits.issued && e.bits.ready)
    val selOH    = PriorityEncoderOH(canIssue)
    val hasSel   = canIssue.reduce(_||_)
    val selIdx   = OHToUInt(selOH)

    val outCmd = Wire(new SimpleRSCmd(ADDR_W))
    outCmd := 0.U.asTypeOf(outCmd)
    when(hasSel) {
      val e = Mux1H(selOH, exQ.map(_.bits))
      outCmd.qType    := e.qType
      outCmd.opa      := e.opa
      outCmd.opb      := e.opb
      outCmd.opaIsDst := e.opaIsDst
    }

    io.issue.ex.valid := hasSel && !io.alloc.valid
    io.issue.ex.cmd   := outCmd
    io.issue.ex.robId := Cat(Q_EX, selIdx.asUInt.pad(IDX_W))

    when(io.issue.ex.fire()) {
      for (i <- 0 until N_EX) {
        when(selOH(i)) { exQ(i).bits.issued := true.B }
      }
      // clear deps_ex(i) across ALL entries
      for (i <- 0 until N_EX) {
        when(selOH(i)) {
          //ldQ.foreach { e => e.bits.deps_ex(i) := false.B }
          exQ.foreach { e => e.bits.deps_ex(i) := false.B }
          //stQ.foreach { e => e.bits.deps_ex(i) := false.B }
        }
      }
    }
  }

  private def issueSt(): Unit = {
    val canIssue = stQ.map(e => e.valid && !e.bits.issued && e.bits.ready)
    val selOH    = PriorityEncoderOH(canIssue)
    val hasSel   = canIssue.reduce(_||_)
    val selIdx   = OHToUInt(selOH)

    val outCmd = Wire(new SimpleRSCmd(ADDR_W))
    outCmd := 0.U.asTypeOf(outCmd)
    when(hasSel) {
      val e = Mux1H(selOH, stQ.map(_.bits))
      outCmd.qType    := e.qType
      outCmd.opa      := e.opa
      outCmd.opb      := e.opb
      outCmd.opaIsDst := e.opaIsDst
    }

    io.issue.st.valid := hasSel && !io.alloc.valid
    io.issue.st.cmd   := outCmd
    io.issue.st.robId := Cat(Q_ST, selIdx.asUInt.pad(IDX_W))

    when(io.issue.st.fire()) {
      for (i <- 0 until N_ST) {
        when(selOH(i)) { stQ(i).bits.issued := true.B }
      }
      // clear deps_st(i) across ALL entries
      for (i <- 0 until N_ST) {
        when(selOH(i)) {
          //ldQ.foreach { e => e.bits.deps_st(i) := false.B }
          //exQ.foreach { e => e.bits.deps_st(i) := false.B }
          stQ.foreach { e => e.bits.deps_st(i) := false.B }
        }
      }
    }
  }

  issueLd(); issueEx(); issueSt()

  // ----------------------------
  // Completion: free the slot and clear deps in all entries for that index of the queue
  // ----------------------------
  when(io.completed.valid) {
    val qType  = io.completed.bits(ROB_ID_WIDTH-1, IDX_W)
    val idx    = io.completed.bits(IDX_W-1, 0)

    when(qType === Q_LD) {
      ldQ(idx).valid := false.B
      ldQ.foreach { e => e.bits.deps_ld(idx) := false.B }
      exQ.foreach { e => e.bits.deps_ld(idx) := false.B }
      stQ.foreach { e => e.bits.deps_ld(idx) := false.B }
    }.elsewhen(qType === Q_EX) {
      exQ(idx).valid := false.B
      ldQ.foreach { e => e.bits.deps_ex(idx) := false.B }
      exQ.foreach { e => e.bits.deps_ex(idx) := false.B }
      stQ.foreach { e => e.bits.deps_ex(idx) := false.B }
    }.elsewhen(qType === Q_ST) {
      stQ(idx).valid := false.B
      ldQ.foreach { e => e.bits.deps_st(idx) := false.B }
      exQ.foreach { e => e.bits.deps_st(idx) := false.B }
      stQ.foreach { e => e.bits.deps_st(idx) := false.B }
    }
  }
}
