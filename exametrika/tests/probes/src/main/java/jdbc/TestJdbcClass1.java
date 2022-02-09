package jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Time;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.DeleteDbFiles;


public class TestJdbcClass1 {
    private final JdbcConnectionPool dataSource;
    private Connection connection;

    public static void main(String[] args) throws Throwable {
        TestJdbcClass1 c = new TestJdbcClass1(0);

        for (int i = 0; i < 10000; i++) {
            long t = System.currentTimeMillis();

            c.test();

            t = System.currentTimeMillis() - t;

            System.out.println("---------------- " + i + ": " + t);
        }
    }

    public TestJdbcClass1(int i) throws Throwable {
        DeleteDbFiles.execute("~", "h2-test" + i, true);

        Class.forName("org.h2.Driver");
        dataSource = JdbcConnectionPool.create("jdbc:h2:~/h2-test" + i, "sa", "");

        Connection connection = dataSource.getConnection();
        Statement stat = connection.createStatement();

        stat.execute("create table test(id int primary key, name varchar(255), gender boolean, weight double, birth_date date, birth_time time)");
        stat.close();
        connection.close();
    }

    public void close() {
        dataSource.dispose();
    }

    public void test() throws Throwable {
        connect();
        update();
        update2();
        query1();
        query2();
        query3();
        query4();
        query5();
        connection.close();
    }

    private void connect() throws Throwable {
        connect2();

        connection = dataSource.getConnection();
        Statement stat = connection.createStatement();
        stat.executeUpdate("delete from test");
        stat.close();
    }

    private void connect2() throws Throwable {
        for (int i = 0; i < 10000; i++) {
            connection = dataSource.getConnection();
            connection.close();
        }
    }

    private void update() throws Throwable {
        Statement statement = connection.createStatement();

        for (int i = 0; i < 5000; i++)
            statement.executeUpdate("insert into test values(" + i + ", '" + ("name" + i) + "', " + ((i % 2 == 0) ? "false" : "true") +
                    ", " + (i * 1.15) + ", '2001-2-3', '14:28:1')");
        statement.close();
    }

    @SuppressWarnings("deprecation")
    private void update2() throws Throwable {
        PreparedStatement statement = connection.prepareStatement("insert into test values(?, ?, ?, ?, ?, ?)");

        for (int i = 5000; i < 10000; i++) {
            statement.setInt(1, i);
            statement.setString(2, "name" + i);
            statement.setBoolean(3, (i % 2 == 0) ? false : true);
            statement.setDouble(4, i * 1.15);
            statement.setDate(5, new Date(2001, 2, 3));
            statement.setTime(6, new Time(14, 28, 1));
            statement.executeUpdate();
        }
        statement.close();
    }

    private void query1() throws Throwable {
        for (int i = 0; i < 100; i++) {
            Statement statement = connection.createStatement();
            query11(statement);
            statement.close();
        }
    }

    private void query3() throws Throwable {
        for (int i = 0; i < 100; i++) {
            PreparedStatement statement = connection.prepareStatement("select * from test where name = ?");
            query31(statement);
            statement.close();
        }
    }

    private void query2() throws Throwable {
        for (int i = 0; i < 100; i++) {
            Statement statement = connection.createStatement();
            query21(statement);
            statement.close();
        }
    }

    private void query4() throws Throwable {
        for (int i = 0; i < 100; i++) {
            PreparedStatement statement = connection.prepareStatement("select * from test where name = ?");
            statement.setString(1, "name" + 2000);
            statement.addBatch();
            statement.setString(1, "name" + 5000);
            statement.addBatch();
            statement.setString(1, "name" + 8000);
            statement.addBatch();
            query41(statement);
            statement.close();
        }
    }

    private void query5() throws Throwable {
        for (int i = 0; i < 10; i++) {
            PreparedStatement statement = connection.prepareStatement("select * from test where name = ?");
            query51(statement);
            statement.close();
        }
    }

    private void query11(Statement statement) throws Throwable {
        for (int i = 0; i < 1000; i++)
            statement.executeQuery("select * from test");
    }

    private void query21(Statement statement) throws Throwable {
        for (int i = 0; i < 1000; i++)
            statement.executeQuery("select * from test where name is '" + "name" + 9000 + "'");
    }

    private void query31(PreparedStatement statement) throws Throwable {
        for (int i = 0; i < 1000; i++) {
            statement.setString(1, "name" + 9000);
            statement.executeQuery();
        }
    }

    private void query41(PreparedStatement statement) throws Throwable {
        for (int i = 0; i < 1000; i++)
            statement.executeQuery();
    }

    private void query51(PreparedStatement statement) throws Throwable {
        for (int i = 0; i < 100; i++) {
            statement.setString(1, "name" + (i % 10));
            statement.executeQuery();
            statement.clearParameters();
        }
    }
}