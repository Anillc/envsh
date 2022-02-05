{ pkgs, ... }: packages: pkgs.substituteAll {
    name = "build.sh";
    src = ./build.sh;
    deps = pkgs.writeText "deps" (pkgs.lib.strings.concatStringsSep " " (packages ++ [ pkgs.bash ]));
    path = pkgs.lib.strings.makeBinPath (packages ++ [ pkgs.bash ]);
    proot = pkgs.proot-static;
    isExecutable = true;
    inherit (pkgs) nix bash runtimeShell;
}