package com.zrlog.plugin.backup.service;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MysqlDumpStatementsTest {

    @Test
    public void shouldSplitDumpWithoutBreakingQuotedSemicolons() throws Exception {
        String dump = "-- dump header\n"
                + "CREATE TABLE `log` (`id` int, `content` text);\n"
                + "INSERT INTO `log` VALUES (1, 'a;b'), (2, \"c;d\");\n"
                + "# footer\n"
                + "LOCK TABLES `log` WRITE;\n"
                + "UNLOCK TABLES;\n";

        List<String> statements = MysqlDumpStatements.read(new StringReader(dump));

        assertEquals(4, statements.size());
        assertEquals("CREATE TABLE `log` (`id` int, `content` text)", statements.get(0));
        assertEquals("INSERT INTO `log` VALUES (1, 'a;b'), (2, \"c;d\")", statements.get(1));
        assertEquals("LOCK TABLES `log` WRITE", statements.get(2));
        assertEquals("UNLOCK TABLES", statements.get(3));
    }

    @Test
    public void shouldIgnoreMysqlVersionCommentsAndPreserveEscapes() throws Exception {
        String dump = "/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;\n"
                + "INSERT INTO `log` VALUES (1, 'it\\'s safe');\n";

        List<String> statements = MysqlDumpStatements.read(new StringReader(dump));

        assertEquals(1, statements.size());
        assertEquals("INSERT INTO `log` VALUES (1, 'it\\'s safe')", statements.get(0));
    }
}
