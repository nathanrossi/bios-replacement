SUMMARY = "A small image just capable of allowing a device to boot."
LICENSE = "MIT"

inherit core-image

IMAGE_ROOTFS_SIZE ?= "8192"
IMAGE_LINGUAS = " "

IMAGE_INSTALL = " \
    base-files \
    base-passwd \
    netbase \
    busybox \
    simpleboot-init \
    ${MACHINE_ESSENTIAL_EXTRA_RDEPENDS} \
    "

