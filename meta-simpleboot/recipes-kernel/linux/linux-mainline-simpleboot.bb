SECTION = "kernel"
DESCRIPTION = "Mainline Linux kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

inherit kernel

# disable kernel-base depending on image, other mechanisms are used to ship the kernel
RDEPENDS:${KERNEL_PACKAGE_NAME}-base = ""
RRECOMMENDS:${KERNEL_PACKAGE_NAME}-base = ""

# elfutils needed for x86 kernels
DEPENDS += "${@bb.utils.contains('ARCH', 'x86', 'elfutils-native', '', d)}"
KERNEL_EXTRA_ARGS:append:x86-64 = " HOST_LIBELF_LIBS='-L${RECIPE_SYSROOT_NATIVE}/usr/lib/pkgconfig/../../../usr/lib/ -lelf' "

DEFAULT_PREFERENCE = "-1"
COMPATIBLE_MACHINE = "^$"

S = "${UNPACKDIR}/${BPN}-${PV}"

SRCREV = "78d82960b939df64cf7d26ca5ed34eb87f44c9e5"
PV = "6.18.2+git${SRCPV}"
SRC_URI = "git://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git;protocol=https;branch=linux-6.18.y"

python do_generate_config() {
    def config(name, val):
        cfgname = "CONFIG_{}".format(name)
        cfgcomment = "# " + cfgname
        data = ""
        matched = False
        with open(d.expand("${B}/.config"), "r") as source:
            for l in source:
                if l.startswith(cfgname + "=") or l.startswith(cfgcomment + " "):
                    data += "{}={}\n".format(cfgname, val)
                    matched = True
                else:
                    data += l
            if not matched: # append if not set
                data += "{}={}\n".format(cfgname, val)

        with open(d.expand("${B}/.config"), "w") as dest:
            dest.write(data)

        # track pending requests
        with open(d.expand("${B}/.config.request"), "a") as f:
            f.write("{}={}\n".format(cfgname, val))

    with open(d.expand("${B}/.config"), "w") as f:
        if d.getVar("KBUILD_DEFCONFIG"):
            with open(d.expand("${S}/arch/${ARCH}/configs/${KBUILD_DEFCONFIG}"), "r") as src:
                f.write(src.read())

    with open(d.expand("${B}/.config.request"), "w") as f:
        f.write("# requested configs\n")
}
addtask generate_config before do_configure after do_unpack do_patch

do_merge_config() {
    cd ${S}
    env \
        CFLAGS="${CFLAGS} ${TOOLCHAIN_OPTIONS}" \
        HOSTCC="${BUILD_CC} ${BUILD_CFLAGS} ${BUILD_LDFLAGS}" \
        HOSTCPP="${BUILD_CPP}" \
        CC="${KERNEL_CC}" \
        LD="${KERNEL_LD}" \
        OBJCOPY="${KERNEL_OBJCOPY}" \
        STRIP="${KERNEL_STRIP}" \
        ARCH=${ARCH} \
        ${S}/scripts/kconfig/merge_config.sh -O ${B} -n ${B}/.config.request
}
addtask merge_config before do_configure after generate_config

python do_check_config() {
    def load_configs(path):
        configs = []
        with open(path, "r") as f:
            for i in f:
                i = i.strip()
                if len(i) == 0 or i.startswith("#"):
                    continue
                parts = i.split("=", 1)
                configs.append((parts[0], parts[1]))
        return configs

    actual = load_configs(d.expand("${B}/.config"))
    requested = load_configs(d.expand("${B}/.config.request"))

    count = 0
    for key, value in requested:
        if not any(key == k for k, _ in actual):
            if value == "n":
                continue # ignore select no, if not present
            bb.warn("Requested config {}={} does not appear in .config".format(key, value))
            count += 1
            continue

        for k, v in actual:
            if k != key:
                continue

            if v != value:
                bb.warn("Requested config {}={} does not match .config of {}={}".format(key, value, k, v))
                count += 1
            break

    if count != 0:
        bb.fatal("Config mismatch")
}
addtask check_config after do_configure before do_compile

COMPATIBLE_MACHINE:x86-64 = ".*"

python do_generate_config:append:simpleboot () {
    config("WERROR", "y")

    config("EXPERT", "y")
    config("SYSVIPC", "y")
    config("POSIX_MQUEUE", "y")
    config("AUDIT", "y")
    config("NO_HZ", "y")
    config("HZ_1000", "y")
    config("POSIX_TIMERS", "y")
    config("HIGH_RES_TIMERS", "y")
    # config("PREEMPT", "y")
    config("PREEMPT_VOLUNTARY", "y")
    config("FUTEX", "y")
    config("TIMERFD", "y")
    config("EVENTFD", "y")
    config("SIGNALFD", "y")
    config("EPOLL", "y")
    config("SHMEM", "y")
    config("RELAY", "y")
    config("KCMP", "y")

    config("BLK_DEV_INITRD", "y")
    config("SMP", "y")
    config("IRQ_REMAP", "y")
    config("HUGETLBFS", "y")

    config("CC_OPTIMIZE_FOR_SIZE", "y")
    # compress kernel with zstd
    config("KERNEL_ZSTD", "y")
    # compress initramfs with zstd
    # config("INITRAMFS_COMPRESSION_ZSTD", "y")
    config("RD_ZSTD", "y")

    # enable kexec
    config("KEXEC", "y")

    config("MODULES", "y")
    config("MODULE_UNLOAD", "y")
    config("MODULE_FORCE_UNLOAD", "y")

    # /proc/config
    config("IKCONFIG", "y")
    config("IKCONFIG_PROC", "y")
    # debug
    config("PRINTK", "y")
    config("PRINTK_TIME", "y")
    config("LOG_BUF_SHIFT", "18")
    config("EARLY_PRINTK", "y")
    # config("EARLYCON", "y")

    config("COMMON_CLK", "y")
    config("PM", "y")
    config("PM_CLK", "y")
    config("CPU_FREQ", "y")
    config("CPU_FREQ_STAT", "y")
    config("CPU_FREQ_DEFAULT_GOV_SCHEDUTIL", "y")
    config("CPU_FREQ_GOV_ONDEMAND", "y")
    config("CPU_FREQ_GOV_PERFORMANCE", "y")
    config("CPU_FREQ_GOV_POWERSAVE", "y")
    config("CPU_FREQ_GOV_SCHEDUTIL", "y")
    config("CPU_IDLE", "y")

    # file systems
    config("PARTITION_ADVANCED", "y")
    config("MSDOS_PARTITION", "y")
    config("EFI_PARTITION", "y")
    config("DEVTMPFS", "y")
    config("DEVTMPFS_MOUNT", "y")
    config("SYSFS", "y")
    config("PROC_FS", "y")
    config("PROC_SYSCTL", "y")
    config("TMPFS", "y")
    config("TMPFS_POSIX_ACL", "y")
    config("BLOCK", "y")
    config("EXT4_FS", "y")
    config("EXT4_FS_POSIX_ACL", "y")
    config("EXT4_FS_SECURITY", "y")
    config("BTRFS_FS", "y")
    config("BTRFS_FS_POSIX_ACL", "y")
    config("ISO9660_FS", "y")
    config("JOLIET", "y")
    config("MSDOS_FS", "y")
    config("VFAT_FS", "y")

    config("BINFMT_ELF", "y")
    config("BINFMT_SCRIPT", "y")
    config("BINFMT_MISC", "y")

    # fs encoding support
    config("NLS", "y")
    config("NLS_DEFAULT", "\"utf8\"")
    config("NLS_CODEPAGE_437", "y")
    config("NLS_ASCII", "y")
    config("NLS_ISO8859_1", "y")
    config("NLS_UTF8", "y")

    # tty
    config("TTY", "y")

    # networking
    config("NET", "y")
    config("UNIX", "y")
    config("PACKET", "y")
    config("INET", "y")
    config("IP_MULTICAST", "y")
    config("IP_ADVANCED_ROUTER", "y")
    config("IP_ROUTE_MULTIPATH", "y")
    config("SYN_COOKIES", "y")
    config("IPV6", "y")
    config("NETDEVICES", "y")
    config("NET_CORE", "y")
    config("ETHERNET", "y")
    config("ETHTOOL_NETLINK", "y")

    config("HW_RANDOM", "y")

    config("IOMMU_SUPPORT", "y")

    config("PCI", "y")
    config("PCI_MSI", "y")
    config("PCI_IOV", "y")
    config("PCIEPORTBUS", "y")
    config("HOTPLUG_PCI", "y")
    config("PCIE_PME", "y")

    config("BLK_PM", "y")
    config("BLK_DEV", "y")
    config("BLK_DEV_SD", "y")
    config("CHR_DEV_SG", "y")
    config("SCSI_CONSTANTS", "y")
    config("SCSI_SPI_ATTRS", "y")
    config("ATA", "y")
    config("SATA_AHCI", "y")
    config("NVME_CORE", "y")
    config("BLK_DEV_NVME", "y")

    config("USB_SUPPORT", "y")
    config("USB", "y")
    config("USB_ANNOUNCE_NEW_DEVICES", "y")
    config("USB_MON", "y")
    config("USB_PCI", "y")
    config("USB_XHCI_HCD", "y")
    config("USB_EHCI_HCD", "y")
    config("USB_OHCI_HCD", "y")
    config("USB_UHCI_HCD", "y")
    config("USB_STORAGE", "y")
    config("USB_UAS", "y")

    config("RTC_CLASS", "y")
    config("RTC_HCTOSYS", "y")
    config("RTC_SYSTOHC", "y")
    config("RTC_INTF_DEV", "y")

    config("HWMON", "y")
}

python do_generate_config:append:x86-64 () {
    # bios/acpi
    config("ACPI", "y")
    config("ACPI_PROCESSOR", "y")
    config("DMI", "y")
    config("DMIID", "y")

    config("EFI", "y")
    config("EFI_ESRT", "y")
    config("EFI_RUNTIME_WRAPPERS", "y")
    config("EFI_EARLYCON", "y")

    config("RELOCATABLE", "y")
    config("64BIT", "y")
    config("MTRR", "y")
    config("X86_MSR", "y")
    config("X86_CPUID", "y")
    config("X86_X2APIC", "y")
    config("X86_CHECK_BIOS_CORRUPTION", "y")
    config("X86_ACPI_CPUFREQ", "y")
    config("X86_AMD_PSTATE", "y")
    config("X86_INTEL_PSTATE", "y")
    config("X86_MCE", "y")
    config("X86_MCE_AMD", "y")
    config("X86_MCE_INTEL", "y")
    config("X86_PLATFORM_DEVICES", "y")
    config("INTEL_IDLE", "y")

    config("KEXEC_FILE", "y")
    # config("KEXEC_JUMP", "y") # required Hibernation support

    # virtio drivers
    config("VIRTIO", "y")
    config("VIRTIO_MENU", "y")
    config("VIRTIO_PCI", "y")
    config("VIRTIO_BLK", "y")
    config("VIRTIO_CONSOLE", "y")
    config("VIRTIO_NET", "y")

    config("SERIAL_8250", "y")
    config("SERIAL_8250_CONSOLE", "y")
    config("SERIAL_8250_NR_UARTS", "32")
    config("SERIAL_8250_EXTENDED", "y")
    config("SERIAL_8250_MANY_PORTS", "y")
    config("SERIAL_8250_SHARE_IRQ", "y")
    config("SERIAL_8250_DETECT_IRQ", "y")
    config("SERIAL_8250_RSA", "y")
    config("SERIAL_8250_PNP", "y")
    config("SERIAL_8250_PCI", "y")
    config("SERIAL_8250_PCILIB", "y")
    config("SERIAL_8250_DW", "y")
    config("SERIAL_8250_DWLIB", "y")

    config("HW_RANDOM_INTEL", "y")
    config("HW_RANDOM_AMD", "y")

    config("HPET", "y")
    config("HPET_TIMER", "y")
    config("WATCHDOG", "y")

    config("AMD_IOMMU", "y")
    config("INTEL_IOMMU", "y")
    # config("INTEL_IOMMU_DEFAULT_ON is not set

    # networking
    config("NET_VENDOR_INTEL", "y")
    # IGC/i225/i226
    config("IGC", "y")
    # i210...
    config("IGB", "y")
    # QEMU
    config("E100", "y")
    config("E1000", "y")
    config("E1000E", "y")

    # thermal/temperature
    config("SENSORS_CORETEMP", "y")

    config("RTC_DRV_CMOS", "y")
}

#do_compile:append() {
#    # Fix up generated filenames in specific files
#    if [ -e ${B}/arch/${ARCH}/include/generated/asm/mach-types.h ]; then
#        sed -i 's#${STAGING_KERNEL_DIR}#${TARGET_DBGSRC_DIR}#' \
#            ${B}/arch/${ARCH}/include/generated/asm/mach-types.h
#    fi
#}
