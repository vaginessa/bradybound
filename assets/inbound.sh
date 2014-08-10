RULE_NAME=%1$s
SPEED=%2$d
ACTION=%3$s

function uninstall {

  # Remove the existing rules from IPv4 table

  iptables -S |
  grep $RULE_NAME |
  sed "s/^-A //" |
  while read rule
  do
    iptables -D $rule
  done >/dev/null &&

  # Remove the existing rules from IPv6 table

  ip6tables -S |
  grep $RULE_NAME |
  sed "s/^-A //" |
  while read rule
  do
    ip6tables -D $rule
  done >/dev/null &&

  return 0
  
  return 1
}

function install {

  # Add new rules to IPv4 table

  iptables -I INPUT 1 -m state --state ESTABLISHED -p tcp \
           -m length --length 30:10000 \
           -m hashlimit --hashlimit-name $RULE_NAME \
           --hashlimit-above "$SPEED"/s -j DROP >/dev/null &&

  #iptables -I FORWARD 1 -m state --state ESTABLISHED -p tcp \
  #         -d 192.0.0.0/24 -m length --length 30:10000 \
  #         -m hashlimit --hashlimit-name $RULE_NAME \
  #         --hashlimit-above "$SPEED"/s -j DROP >/dev/null &&

  # Add new rules to IPv6 table

  ip6tables -I INPUT 1 -m state --state ESTABLISHED -p tcp \
           -m length --length 40:10000 \
           -m hashlimit --hashlimit-name $RULE_NAME \
           --hashlimit-above "$SPEED"/s -j DROP >/dev/null &&

  return 0

  return 1

}

if [ "$ACTION" = install ]
then
  (uninstall && install && echo -n 1) || uninstall
else
  uninstall && echo -n 1
fi
