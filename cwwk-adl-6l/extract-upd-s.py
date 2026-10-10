import os
import sys
import struct

def find_upd_section(data, name):
	name = name.encode()
	print("Search for {}".format(repr(name)))
	offset = None
	for i in range(len(data)):
		if all(data[i + ji] == j for ji, j in enumerate(name)):
			offset = i - 4
			break

	if offset is None:
		raise Exception("UPD not found")
	print("found upd at offset 0x{:x}".format(offset))

	upd = data[offset:]
	print(upd[0:16].hex(" "))

	# base upd structure (@0x0)
	#   - u16 - size??
	#   - u8  - ??
	#   - u8  - ??
	#   - u64 - signature
	#   - u8  - rev
	#   - u8 * 23 - reserved
	updsize = upd[0] + (upd[1] << 8)
	upd = upd[0:updsize]
	print("upd size = {} bytes 0x{:x}".format(updsize, updsize))
	updrev = upd[12]
	print("upd rev = 0x{:x}".format(updrev))

	# with open("dump.upd.bin", "wb") as f:
		# f.write(upd)

	if upd[-1] != 0x55 or upd[-2] != 0xaa:
		raise Exception("Did not find UPD terminator at end of data")

	# arch upd structure (@0x24)
	#   - u8  - rev
	#   - u8 * 3 - reserved
	#   - u32 - nvs
	#   - u32 - stack base
	#   - u32 - stack size
	#   - u32 - bootloader
	#   - u32 - bootmode
	#   - u8 * 8 - reserved
	# arch_upd = upd[0x24:0x44]

	return upd[0x4:]

if __name__ == "__main__":
	args = sys.argv[1:]

	with open(args[0], "rb") as f:
		data = memoryview(f.read())
	print("Image size = {} bytes".format(len(data)))

	headerpath = "/storage/dev/coreboot/src/vendorcode/intel/fsp/fsp2_0/alderlake_n/FspsUpd.h"

	print("=== FSP_S_CONFIG ===")
	fsp_s = find_upd_section(data, "ADLUPD_S")

	print("== PCH")
	for i in range(28):
		print("PcieRpEnableCpm[{}] = {}".format(i, fsp_s[0x04bf + i]))
	for i in range(28):
		print("PcieRpSlotImplemented[{}] = {}".format(i, fsp_s[0x0487 + i]))
	# for i in range(28):
		# print("PcieRpSlotAcsEnabled[{}] = {}".format(i, fsp_s[0x04a3 + i]))
	for i in range(28):
		print("PcieRpHotPlug[{}] = {}".format(i, fsp_s[0x075b + i]))
	for i in range(28):
		print("PcieRpClkReqDetect[{}] = {}".format(i, fsp_s[0x07b1 + i]))
	# for i in range(28):
		# print("PcieRpAER[{}] = {}".format(i, fsp_s[0x07cd + i]))
	for i in range(28):
		print("PcieRpPcieSpeed[{}] = {}".format(i, fsp_s[0x08d9 + i]))
	for i in range(28):
		# ASPM_DEFAULT = 0
		# ASPM_DISABLE = 1
		# ASPM_L0S = 2
		# ASPM_L1 = 3
		# ASPM_L0S_L1 = 4
		# ASPM_AUTO = 5
		print("PcieRpAspm[{}] = {}".format(i, fsp_s[0x092d + i]))
	for i in range(28):
		print("PcieRpL1Substates[{}] = {}".format(i, fsp_s[0x0949 + i]))
	for i in range(28):
		print("PcieRpLtrEnable[{}] = {}".format(i, fsp_s[0x0981 + i]))

	print("PchPcieClockGating = {}".format(fsp_s[0x9dd]))
	print("PchPciePowerGating = {}".format(fsp_s[0xa81]))

	print("== CPU")
	for i in range(4):
		print("CpuPcieRpEnableCpm[{}] = {}".format(i, fsp_s[0x0c38 + i]))
	for i in range(4):
		print("CpuPcieRpSlot[{}] = {}".format(i, fsp_s[0xc68 + i]))
	for i in range(4):
		print("CpuPcieClockGating[{}] = {}".format(i, fsp_s[0xc15 + i]))
	for i in range(4):
		print("CpuPciePowerating[{}] = {}".format(i, fsp_s[0xc19 + i]))
	for i in range(4):
		print("CpuPcieRpAspm[{}] = {}".format(i, fsp_s[0xc78 + i]))
	for i in range(4):
		print("CpuPcieRpL1Substates[{}] = {}".format(i, fsp_s[0xc7c + i]))
	for i in range(4):
		print("CpuPcieRpLtrEnable[{}] = {}".format(i, fsp_s[0xc80 + i]))

	print("== sata")
	for i in range(8):
		print("SataPortsEnable[{}] = {}".format(i, fsp_s[0x0062 + i]))
	for i in range(8):
		print("SataPortsDevSlp[{}] = {}".format(i, fsp_s[0x006a + i]))
	print("SataSalpSupport = {}".format(fsp_s[0x0061]))


	print("Eist = {}".format(fsp_s[0x0d28]))
	print("EnergyEfficientTurbo = {}".format(fsp_s[0x0d2a]))
	print("PmcPdEnable = {}".format(fsp_s[0x05d1]))
	value = struct.unpack("<H", fsp_s[0x05d2:][0:2])[0]
	print("TcssAuxOri = {}".format(value))
	value = struct.unpack("<H", fsp_s[0x05d4:][0:2])[0]
	print("TcssHslOri = {}".format(value))

	print("UsbTcPortEn = {}".format(fsp_s[0x05db]))
	for i in range(4):
		print("EnableTcssCovTypeA[{}] = {}".format(i, fsp_s[0x0fee + i]))
	for i in range(4):
		print("MappingPchXhciUsbA[{}] = {}".format(i, fsp_s[0x0ff2 + i]))
	print("D3HotEnable = {}".format(fsp_s[0x051a]))
	print("D3ColdEnable = {}".format(fsp_s[0x0545]))
	print("Usb4CmMode = {}".format(fsp_s[0x0cfc]))

	for i in range(4):
		value = struct.unpack("<I", fsp_s[0x051c + (4 * i):][0:4])[0]
		print("IomTypeCPortPadCfg[{}] = 0x{:x}".format(i, value))

	print("=== FSP_M_CONFIG ===")
	fsp_m = find_upd_section(data, "ADLUPD_M")

	for i in range(18):
		print("PcieClkSrcUsage[{}] = {}".format(i, fsp_m[0x0749 + i]))
		# print("PcieClkSrcUsageRsvd[{}] = {}".format(i, fsp_m[0x075b + i]))
		print("PcieClkSrcClkReq[{}] = {}".format(i, fsp_m[0x769 + i]))
		# print("PcieClkSrcReqRsvd[{}] = {}".format(i, fsp_m[0x77b + i]))

	mask = struct.unpack("<I", fsp_m[0x7d8:][0:4])[0]
	print("PcieRpEnableMask = 0x{:x}".format(mask))

	print("DdiPortAConfig = 0x{:x}".format(fsp_m[0x0279]))
	print("DdiPortBConfig = 0x{:x}".format(fsp_m[0x027a]))
	for i, name in enumerate(["A", "B", "C", "1", "2", "3", "4"]):
		print("DdiPort {} Hpd = {}".format(name, fsp_m[0x027b + i]))
	for i, name in enumerate(["A", "B", "C", "1", "2", "3", "4"]):
		print("DdiPort {} Ddc = {}".format(name, fsp_m[0x0282 + i]))

	print("EnableC6Dram = 0x{:x}".format(fsp_m[0x03b5]))
	print("SaGv = 0x{:x}".format(fsp_m[0x01d4]))

	print("UsbTcPortEnPreMem = 0x{:x}".format(fsp_m[0x088e]))
	print("TcssXhciEn = 0x{:x}".format(fsp_m[0x082b]))
	print("TcssXdciEn = 0x{:x}".format(fsp_m[0x082c]))
	print("TcssDma0En = 0x{:x}".format(fsp_m[0x082d]))
	print("TcssDma1En = 0x{:x}".format(fsp_m[0x082e]))

