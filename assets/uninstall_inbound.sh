RULE_NAME=%1$s

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

echo -n 1
