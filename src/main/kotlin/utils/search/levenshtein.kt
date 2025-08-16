package utils.search

fun levenshtein(a: String, b: String): Int {
	val m = a.length
	val n = b.length
	val dp = Array(m + 1) { IntArray(n + 1) }

	for (i in 0..m) dp[i][0] = i
	for (j in 0..n) dp[0][j] = j

	for (i in 1..m) {
		for (j in 1..n) {
			val cost = if (a[i - 1] == b[j - 1]) 0 else 1
			dp[i][j] = minOf(
				dp[i - 1][j] + 1,       // löschen
				dp[i][j - 1] + 1,       // einfügen
				dp[i - 1][j - 1] + cost // ersetzen
			)
		}
	}

	return dp[m][n]
}