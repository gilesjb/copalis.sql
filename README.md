### A Simple Query

Wouldn't it be nice if you could declare a method on an interface,
annotate it with a SQL query,
and there was a toolkit that would generate an implementation for you?

    interface QuerySession extends Session {
        @Query("select ZIP from ADDRESSES where NAME=$1") String zipCode(String name);
    }
    
    public static void main(String[] args] {
        QuerySession session = Connecting.to("jdbc://my-database").as(QuerySession.class).open();
        System.out.println("Smith's zip code is: " + session.zipCode("Smith"));
        session.close();
    }

### A Less Simple Query

Most of the time when we perform a SQL SELECT,
we may get more than one record,
and want to read multiple fields.
To do this in copalis.sql,
define an interface extending `Results` for accessing the field values:

    interface Addresses extends Results {
        String name();
        String street();
        String city();
        String state();
        String zip();
    }
    
    interface AddrSession extends Session {
        @Query("select * from ADDRESSES where NAME=$1") Addresses forName(String name);
    }

    public static void main(String[] args] {
        AddrSession session = Connecting.to("jdbc://my-database").as(AddrSession.class).open();
        Addresses addrs = session.forName("Jones");
        while (addrs.next()) {
            System.out.println(addrs.name() + " lives in city: " + addrs.city());
        }
        addrs.close();
        session.close();
    }
    
### Updates, Inserts

copalis.sql does those too.
Check out out the samples.

### Credits

copalis.sql was inspired by [TMDBC](https://tmdbc.dev.java.net/), but departs from it in these respects:

+ Uses standard JDBC and is not tied to Postgres
+ Uses runtime dynamic proxies rather than compile-time code generation
+ Data access interfaces are wrappers around `ResultSet`, not Java Beans