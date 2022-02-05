nixpkgs: { pkgs, ... }: (pkgs.pkgsStatic.callPackage "${nixpkgs}/pkgs/tools/system/proot" { enablePython = false; }).overrideAttrs (old: {
    postPatch = old.postPatch + ''
        sed -i "1i LDFLAGS += -static -ltalloc" src/GNUmakefile
    '';
    postInstall = "";
})