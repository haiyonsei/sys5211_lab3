
`timescale 1ns/1ps

/**
 * DUT: SimpleReservationStation
 * 이 테스트벤치는 제공된 Verilog 인터페이스를 기반으로 SimpleReservationStation의 동작을 검증합니다.
 * 원본 Chisel 테스트 코드의 세 가지 주요 시나리오를 SystemVerilog로 변환했습니다.
 */
module tb_SimpleReservationStation;

  // 파라미터
  localparam ADDR_WIDTH = 16;
  localparam ROB_ID_WIDTH = 3; // DUT 인터페이스에 맞춰 3으로 수정
  localparam CLK_PERIOD = 10;

  // 명령어 타입 상수
  localparam [1:0] Q_LD = 2'd0;
  localparam [1:0] Q_EX = 2'd1;
  localparam [1:0] Q_ST = 2'd2;

  // 클럭 및 리셋 신호
  logic clk;
  logic reset; // Active-high reset

  // DUT 인터페이스 신호
  // Alloc Interface
  logic                       io_alloc_ready;
  logic                       io_alloc_valid;
  logic [1:0]                 io_alloc_bits_qType;
  logic                       io_alloc_bits_opa_valid;
  logic [ADDR_WIDTH-1:0]      io_alloc_bits_opa_start;
  logic [ADDR_WIDTH-1:0]      io_alloc_bits_opa_len;
  logic                       io_alloc_bits_opb_valid;
  logic [ADDR_WIDTH-1:0]      io_alloc_bits_opb_start;
  logic [ADDR_WIDTH-1:0]      io_alloc_bits_opb_len;
  logic                       io_alloc_bits_opaIsDst;

  // Completion Interface
  logic                       io_completed_valid;
  logic [ROB_ID_WIDTH-1:0]    io_completed_bits;

  // Issue LD Interface
  logic                       io_issue_ld_valid;
  logic                       io_issue_ld_ready;
  logic [1:0]                 io_issue_ld_cmd_qType;
  logic                       io_issue_ld_cmd_opa_valid;
  logic [ADDR_WIDTH-1:0]      io_issue_ld_cmd_opa_start;
  logic [ADDR_WIDTH-1:0]      io_issue_ld_cmd_opa_len;
  logic                       io_issue_ld_cmd_opb_valid;
  logic [ADDR_WIDTH-1:0]      io_issue_ld_cmd_opb_start;
  logic [ADDR_WIDTH-1:0]      io_issue_ld_cmd_opb_len;
  logic                       io_issue_ld_cmd_opaIsDst;
  logic [ROB_ID_WIDTH-1:0]    io_issue_ld_robId;

  // Issue EX Interface
  logic                       io_issue_ex_valid;
  logic                       io_issue_ex_ready;
  logic [1:0]                 io_issue_ex_cmd_qType;
  logic                       io_issue_ex_cmd_opa_valid;
  logic [ADDR_WIDTH-1:0]      io_issue_ex_cmd_opa_start;
  logic [ADDR_WIDTH-1:0]      io_issue_ex_cmd_opa_len;
  logic                       io_issue_ex_cmd_opb_valid;
  logic [ADDR_WIDTH-1:0]      io_issue_ex_cmd_opb_start;
  logic [ADDR_WIDTH-1:0]      io_issue_ex_cmd_opb_len;
  logic                       io_issue_ex_cmd_opaIsDst;
  logic [ROB_ID_WIDTH-1:0]    io_issue_ex_robId;

  // Issue ST Interface
  logic                       io_issue_st_valid;
  logic                       io_issue_st_ready;
  logic [1:0]                 io_issue_st_cmd_qType;
  logic                       io_issue_st_cmd_opa_valid;
  logic [ADDR_WIDTH-1:0]      io_issue_st_cmd_opa_start;
  logic [ADDR_WIDTH-1:0]      io_issue_st_cmd_opa_len;
  logic                       io_issue_st_cmd_opb_valid;
  logic [ADDR_WIDTH-1:0]      io_issue_st_cmd_opb_start;
  logic [ADDR_WIDTH-1:0]      io_issue_st_cmd_opb_len;
  logic                       io_issue_st_cmd_opaIsDst;
  logic [ROB_ID_WIDTH-1:0]    io_issue_st_robId;
  
  // Busy Status
  logic                       io_busy;


  // DUT (Device Under Test) 인스턴스화
  SimpleReservationStation dut (
    .clock(clk),
    .reset(reset),

    .io_alloc_ready(io_alloc_ready),
    .io_alloc_valid(io_alloc_valid),
    .io_alloc_bits_qType(io_alloc_bits_qType),
    .io_alloc_bits_opa_valid(io_alloc_bits_opa_valid),
    .io_alloc_bits_opa_start(io_alloc_bits_opa_start),
    .io_alloc_bits_opa_len(io_alloc_bits_opa_len),
    .io_alloc_bits_opb_valid(io_alloc_bits_opb_valid),
    .io_alloc_bits_opb_start(io_alloc_bits_opb_start),
    .io_alloc_bits_opb_len(io_alloc_bits_opb_len),
    .io_alloc_bits_opaIsDst(io_alloc_bits_opaIsDst),

    .io_completed_valid(io_completed_valid),
    .io_completed_bits(io_completed_bits),

    .io_issue_ld_valid(io_issue_ld_valid),
    .io_issue_ld_ready(io_issue_ld_ready),
    .io_issue_ld_cmd_qType(io_issue_ld_cmd_qType),
    .io_issue_ld_cmd_opa_valid(io_issue_ld_cmd_opa_valid),
    .io_issue_ld_cmd_opa_start(io_issue_ld_cmd_opa_start),
    .io_issue_ld_cmd_opa_len(io_issue_ld_cmd_opa_len),
    .io_issue_ld_cmd_opb_valid(io_issue_ld_cmd_opb_valid),
    .io_issue_ld_cmd_opb_start(io_issue_ld_cmd_opb_start),
    .io_issue_ld_cmd_opb_len(io_issue_ld_cmd_opb_len),
    .io_issue_ld_cmd_opaIsDst(io_issue_ld_cmd_opaIsDst),
    .io_issue_ld_robId(io_issue_ld_robId),

    .io_issue_ex_valid(io_issue_ex_valid),
    .io_issue_ex_ready(io_issue_ex_ready),
    .io_issue_ex_cmd_qType(io_issue_ex_cmd_qType),
    .io_issue_ex_cmd_opa_valid(io_issue_ex_cmd_opa_valid),
    .io_issue_ex_cmd_opa_start(io_issue_ex_cmd_opa_start),
    .io_issue_ex_cmd_opa_len(io_issue_ex_cmd_opa_len),
    .io_issue_ex_cmd_opb_valid(io_issue_ex_cmd_opb_valid),
    .io_issue_ex_cmd_opb_start(io_issue_ex_cmd_opb_start),
    .io_issue_ex_cmd_opb_len(io_issue_ex_cmd_opb_len),
    .io_issue_ex_cmd_opaIsDst(io_issue_ex_cmd_opaIsDst),
    .io_issue_ex_robId(io_issue_ex_robId),

    .io_issue_st_valid(io_issue_st_valid),
    .io_issue_st_ready(io_issue_st_ready),
    .io_issue_st_cmd_qType(io_issue_st_cmd_qType),
    .io_issue_st_cmd_opa_valid(io_issue_st_cmd_opa_valid),
    .io_issue_st_cmd_opa_start(io_issue_st_cmd_opa_start),
    .io_issue_st_cmd_opa_len(io_issue_st_cmd_opa_len),
    .io_issue_st_cmd_opb_valid(io_issue_st_cmd_opb_valid),
    .io_issue_st_cmd_opb_start(io_issue_st_cmd_opb_start),
    .io_issue_st_cmd_opb_len(io_issue_st_cmd_opb_len),
    .io_issue_st_cmd_opaIsDst(io_issue_st_cmd_opaIsDst),
    .io_issue_st_robId(io_issue_st_robId),
    
    .io_busy(io_busy)
  );

  // 클럭 생성기
  initial begin
    clk = 0;
    forever #(CLK_PERIOD/2) clk = ~clk;
  end

  // ==================================================
  // =============== Helper Tasks =====================
  // ==================================================

  // DUT 리셋 태스크
  task reset_dut();
    reset <= 1'b1;
    repeat(2) @(posedge clk);
    reset <= 1'b0;
    $display("[%0t] DUT Reset", $time);
  endtask

  // 명령어 할당 태스크
  task poke_cmd(
    input [1:0] q,
    input logic opaValid,
    input [ADDR_WIDTH-1:0] aStart,
    input [ADDR_WIDTH-1:0] aLen,
    input logic opbValid = 0,
    input [ADDR_WIDTH-1:0] bStart = 0,
    input [ADDR_WIDTH-1:0] bLen = 0,
    input logic opaIsDst = 0
  );
    io_alloc_valid <= 1'b1;
    io_alloc_bits_qType <= q;
    io_alloc_bits_opa_valid <= opaValid;
    io_alloc_bits_opa_start <= aStart;
    io_alloc_bits_opa_len <= aLen;
    io_alloc_bits_opb_valid <= opbValid;
    io_alloc_bits_opb_start <= bStart;
    io_alloc_bits_opb_len <= bLen;
    io_alloc_bits_opaIsDst <= opaIsDst;
    
    wait(io_alloc_ready);
    @(posedge clk);
    io_alloc_valid <= 1'b0;
  endtask

  // ==================================================
  // ================= Test Tasks =====================
  // ==================================================

  // 테스트 1: RAW 해저드
  task test_raw_hazard();
    logic [ROB_ID_WIDTH-1:0] ld_rob;
    bit ld_issued = 0;
    bit ex_issued = 0;
    integer i;
    
    $display("[%0t] Starting test: 'block EX (RAW) until LD completes'", $time);
    poke_cmd(Q_LD, 1'b1, 16'd0, 16'd8);
    for (i = 0; i < 8; i = i + 1) begin
      @(posedge clk);
      if (io_issue_ld_valid) begin
        ld_rob = io_issue_ld_robId;
        ld_issued = 1;
        $display("[%0t] LD issued with ROB ID %d", $time, ld_rob);
        break;
      end
    end
    if (!ld_issued) $fatal(1, "[%0t] FATAL: LD did not issue", $time);
    poke_cmd(Q_EX, 1'b1, 16'd0, 16'd8);
    for (i = 0; i < 4; i = i + 1) begin
      @(posedge clk);
      if (io_issue_ex_valid) $fatal(1, "[%0t] FATAL: EX issued before LD completion (RAW violation)", $time);
    end
    $display("[%0t] PASS: EX correctly blocked", $time);
    io_completed_valid <= 1'b1;
    io_completed_bits <= ld_rob;
    @(posedge clk);
    io_completed_valid <= 1'b0;
    $display("[%0t] Signaled completion for ROB ID %d", $time, ld_rob);
    for (i = 0; i < 6; i = i + 1) begin
      @(posedge clk);
      if (io_issue_ex_valid) begin
        ex_issued = 1;
        break;
      end
    end
    if (!ex_issued) $fatal(1, "[%0t] FATAL: EX should issue after LD completion", $time);
    $display("[%0t] PASS: EX issued after LD completion", $time);
  endtask

  // 테스트 2: LD 큐 순차적 실행
  task test_in_order_issue();
    logic [ROB_ID_WIDTH-1:0] ld0_rob, ld1_rob;
    $display("[%0t] Starting test: 'enforce in-order ISSUE within LD queue'", $time);
    poke_cmd(Q_LD, 1'b1, 16'd0, 16'd4);
    poke_cmd(Q_LD, 1'b1, 16'd16, 16'd4);
    @(posedge clk);
    if (!io_issue_ld_valid) $fatal(1, "[%0t] FATAL: LD0 did not issue", $time);
    ld0_rob = io_issue_ld_robId;
    $display("[%0t] LD0 issued with ROB ID %d", $time, ld0_rob);
    @(posedge clk);
    if (!io_issue_ld_valid) $fatal(1, "[%0t] FATAL: LD1 did not issue", $time);
    ld1_rob = io_issue_ld_robId;
    $display("[%0t] LD1 issued with ROB ID %d", $time, ld1_rob);
    if (ld1_rob == ld0_rob) $fatal(1, "[%0t] FATAL: Second LD should have a different ROB id", $time);
    $display("[%0t] PASS: LD queue issued in-order", $time);
  endtask

  // 테스트 3: EX-ST 데이터 의존성
  task test_dependency_hazard();
    logic [ROB_ID_WIDTH-1:0] ex_rob;
    bit st_issued = 0;
    integer i;
    $display("[%0t] Starting test: 'block ST (Data Hazard) until EX completes'", $time);
    poke_cmd(Q_EX, 1'b1, 16'd32, 16'd8, .opaIsDst(1'b1));
    poke_cmd(Q_ST, 1'b1, 16'd32, 16'd8);
    @(posedge clk);
    if (!io_issue_ex_valid) $fatal(1, "[%0t] FATAL: EX-preload did not issue", $time);
    ex_rob = io_issue_ex_robId;
    $display("[%0t] EX-preload issued with ROB ID %d", $time, ex_rob);
    for (i = 0; i < 3; i = i + 1) begin
      @(posedge clk);
      if (io_issue_st_valid) $fatal(1, "[%0t] FATAL: ST issued before EX-preload completion", $time);
    end
    $display("[%0t] PASS: ST correctly blocked", $time);
    io_completed_valid <= 1'b1;
    io_completed_bits <= ex_rob;
    @(posedge clk);
    io_completed_valid <= 1'b0;
    $display("[%0t] Signaled completion for ROB ID %d", $time, ex_rob);
    for (i = 0; i < 6; i = i + 1) begin
        @(posedge clk);
        if (io_issue_st_valid) begin
            st_issued = 1;
            break;
        end
    end
    if (!st_issued) $fatal(1, "[%0t] FATAL: STORE should issue after EX-preload completion", $time);
    $display("[%0t] PASS: ST issued after EX-preload completion", $time);
  endtask

  // ==================================================
  // ================= Main Sequence ==================
  // ==================================================
  initial begin
    $display("Starting SystemVerilog Testbench for SimpleReservationStation...");
    
    // 초기화
    reset_dut();
    io_alloc_valid <= 1'b0;
    io_issue_ld_ready <= 1'b0;
    io_issue_ex_ready <= 1'b0;
    io_issue_st_ready <= 1'b0;
    io_completed_valid <= 1'b0;
    
    // --- 테스트 1 실행 ---
    io_issue_ld_ready <= 1'b1;
    io_issue_ex_ready <= 1'b1;
    io_issue_st_ready <= 1'b1;
    test_raw_hazard();
    @(posedge clk);
    $display("\n--------------------------------\n");
    
    // --- 테스트 2 실행 ---
    reset_dut();
    io_issue_ld_ready <= 1'b1;
    io_issue_ex_ready <= 1'b1;
    io_issue_st_ready <= 1'b1;
    test_in_order_issue();
    @(posedge clk);
    $display("\n--------------------------------\n");
    
    // --- 테스트 3 실행 ---
    reset_dut();
    io_issue_ld_ready <= 1'b1;
    io_issue_ex_ready <= 1'b1;
    io_issue_st_ready <= 1'b1;
    test_dependency_hazard();

    $display("\nAll tests completed successfully.");
    $finish;
  end

endmodule