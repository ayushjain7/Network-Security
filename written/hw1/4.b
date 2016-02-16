define dh() {
	p = 2^61 - 1;
	g = 23489;
	sa = 93573;
	sb = 23903;
    ta = g^sa % p;
    tb = g^sb % p;
    k1 = tb^sa % p;
    k2 = ta^sb % p;
	print "Key is:";
	print k1;
	print "\n";
	return k1 == k2;
}
