public class LibraryCatalogTest {
    static int failed = 0;
    static void check(boolean ok, String n) { System.out.println((ok ? "PASS " : "FAIL ") + n); if (!ok) failed++; }
    public static void main(String[] a) {
        LibraryCatalog c = new LibraryCatalog();
        check(c.search("", "All").size() == 20, "empty search returns all 20 books");
        check(c.search("java", "All").size() == 3, "keyword 'java' (case-insensitive) finds 3");
        check(c.search("", "Mathematics").size() == 4, "category filter Mathematics = 4");
        check(c.search("kalam", "Biography").size() == 2, "author search + category = 2");
        check(c.search("zzz", "All").isEmpty(), "no match -> empty");
        check(LibraryCatalog.pageCount(20, 5) == 4 && LibraryCatalog.pageCount(0, 5) == 1 && LibraryCatalog.pageCount(21, 5) == 5, "page counts");
        check(LibraryCatalog.page(c.search("", "All"), 3, 5).size() == 5, "last full page has 5");
        check(LibraryCatalog.page(c.search("", "All"), 9, 5).isEmpty(), "out-of-range page is empty");
        System.out.println(failed == 0 ? "ALL TESTS PASSED" : failed + " FAILED");
        System.exit(failed);
    }
}
