echo "::total value=3"
echo "::progress value=1"
wget "https://github.com/GrandpaScout/FiguraRewriteVSDocs/archive/master.tar.gz" -O ./__docs.tar.gz 2> /dev/null

echo "::progress value=2"
tar --extract --file="./__docs.tar.gz" FiguraRewriteVSDocs-latest/src --strip-components 2 -C "." > /dev/null

echo "::progress value=3"
rm -f "__docs.tar.gz"