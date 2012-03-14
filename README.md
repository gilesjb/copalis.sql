### A Simple Query

Wouldn't it be nice if you could declare a method on an interface,
annotate it with a SQL query,
and there was a toolkit that would generate an implementation for you?

    interface QuerySession extends Session {
        @Query("select ZIP from ADDRESSES where NAME=$1")
        String zipCode(String name);
    }
    
    public static void main(String[] args] {
        QuerySession session = Connecting.to("jdbc://my-database").open(QuerySession.class);
        System.out.println("Smith's zip code is: " + session.zipCode("Smith"));
        session.close();
    }

### A Less Simple Query

When we query a databse,
we might want to get more than one record,
or read multiple fields.
Define an interface with accessors for the field values,
and return it from the query method:

    interface Addresses extends Results {
        String name();
        String street();
        String city();
        String state();
        String zip();
    }
    
    interface AddrSession extends Session {
        @Query("select * from ADDRESSES where NAME=$1")
        Addresses forName(String name);
    }

    public static void main(String[] args] {
        AddrSession session = Connecting.to("jdbc://my-database").open(AddrSession.class);
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