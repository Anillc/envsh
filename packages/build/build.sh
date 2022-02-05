#!@runtimeShell@
DIR_NAME=$(cat /dev/urandom | tr -dc 'a-z0-9' | fold -w 10 | head -n 1)
PROOT=@proot@/bin/proot
cat << EOF
#!/usr/bin/env bash
PROOT=/tmp/$DIR_NAME/${PROOT:1}
if [ ! -d "/tmp/$DIR_NAME" ]; then
    mkdir -p /tmp/$DIR_NAME
    STORE_TAR_LINE=\$(awk '/^__STORE_TAR__$/ { print NR + 1 }' \$0)
    tail -n +\${STORE_TAR_LINE} \$0 | tar -zxp -C /tmp/$DIR_NAME
fi
export PATH=\$PATH:@path@
\$PROOT -b /tmp/$DIR_NAME/nix:/nix @bash@/bin/bash "\$@"
exit 0
__STORE_TAR__
EOF
echo $(@nix@/bin/nix-store -qR @deps@) | xargs tar czf - $PROOT | cat