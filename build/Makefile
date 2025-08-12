

TB              := ../src/test/verilog/tb_reservation_station.sv 
#../src/main/resources/vsrc/plusarg_reader.v
GEN_SRCS 		:= SimpleReservationStation.v
SRCS			:= ../src/main/verilog/SimpleReservationStation.v 

simv: 
	vcs -full64 -sverilog -timescale=1ns/1ps $(TB) $(SRCS) -debug_access+all 

gui: $(SRCS) $(TB)
	vcs -full64 -sverilog -timescale=1ns/1ps $(SRCS) $(TB) -debug_access+all -gui -kdb

ref: $(GEN_SRCS) $(TB)
	vcs -full64 -sverilog -timescale=1ns/1ps $(TB) $(GEN_SRCS) -debug_access+all 

refgui: $(GEN_SRCS) $(TB)
	vcs -full64 -sverilog -timescale=1ns/1ps $(TB) $(GEN_SRCS) -debug_access+all -gui -kdb

SimpleReservationStation.v:
	cd ..; java -jar /hai/projects/haf/202507/chipyard/generators/rocket-chip/sbt-launch.jar "runMain npu.EmitVerilog --target-dir build"; cd build

run:
	./simv

clean:
	rm -rf *.v *.f ./simv* ./project *.fir *.json  novas*.* *.fsdb verdiLog ucli.key csrc target 
