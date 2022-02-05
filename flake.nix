{
    inputs.nixpkgs.url = "github:nixos/nixpkgs/nixpkgs-unstable";
    inputs.flake-utils = {
        url = "github:numtide/flake-utils";
        inputs.nixpkgs.follows = "nixpkgs";
    };

    outputs = { self, nixpkgs, flake-utils }: flake-utils.lib.eachDefaultSystem (system: let
        pkgs = import nixpkgs {
            overlays = [ overlay ];
            inherit system;
        };
        overlay = self: super: {
            proot-static = import ./packages/proot-static nixpkgs pkgs;
            build = import ./packages/build pkgs;
        };
        input = let
            env = builtins.getEnv "PACKAGES";
            names = pkgs.lib.strings.splitString "," env;
        in builtins.map (x: pkgs.${x}) names;
    in {
        apps.envsh = flake-utils.lib.mkApp rec {
            name = "environment.sh";
            drv = pkgs.build input;
            exePath = "";
        };
        packages.prebuild = pkgs.writeText "packages"
            (pkgs.lib.strings.concatStringsSep " " (builtins.map builtins.toString input));
        devShell = pkgs.mkShell {
            buildInputs = with pkgs; [
                cargo rustc rustfmt rust-analyzer
            ];
            RUST_SRC_PATH = "${pkgs.rust.packages.stable.rustPlatform.rustLibSrc}";
        };
    });
}
