RULE_NAME=%1$s
SPEED=%2$d

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

# Add new rules to IPv4 table

iptables -I INPUT 1 -m state --state ESTABLISHED -p tcp \
         -m length --length 30:10000 \
         -m hashlimit --hashlimit-name $RULE_NAME \
         --hashlimit-above "$SPEED"/s -j DROP >/dev/null &&

# Add new rules to IPv6 table

ip6tables -I INPUT 1 -m state --state ESTABLISHED -p tcp \
         -m length --length 40:10000 \
         -m hashlimit --hashlimit-name $RULE_NAME \
         --hashlimit-above "$SPEED"/s -j DROP >/dev/null &&

echo -n 1
