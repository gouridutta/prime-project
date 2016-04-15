package pp;

import pp.controllers.*;

import java.sql.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

import static spark.Spark.*;

public class PrimeProject
{
	private static final boolean DEBUG = false; //set this to FALSE in non-development builds

	public static void main(String[] args)
	{
		port(8080);

		before("/debug/*", (req, res) -> {
			if( !DEBUG )
				halt(404, "File Not Found");
		});

		get("/", (req, res) -> {
			final Controller ctrl = new HomepageController();
			ctrl.initController(req, res);
			ctrl.executeController();
			ctrl.deinitController();
			return res.body();
		});

		get("/cart", (req, res) -> {
			final Controller ctrl = new CartController();
			ctrl.initController(req, res);
			ctrl.executeController();
			ctrl.deinitController();
			return res.body();
		});

		get("/item/:id", (req, res) -> {
			final Controller ctrl = new ItemController();
			ctrl.initController(req, res);
			ctrl.executeController();
			ctrl.deinitController();
			return res.body();
		});

		staticRoute("/static", "/www/static");

		get("/debug/db", (req, res) -> {
			try( InputStream in = PrimeProject.class.getResourceAsStream("/www/debug/db.html");
				ByteArrayOutputStream out = new ByteArrayOutputStream() )
			{
				res.status(200);
				res.header("Content-Type", "text/html");
				Utils.copyStream(in, out);
				byte[] fileData = out.toByteArray();
				return new String(fileData, StandardCharsets.UTF_8);
			}
		});

		get("/debug/dbQuery", (req, res) -> {
			final Controller ctrl = new DBQueryDebugController();
			ctrl.initController(req, res);
			ctrl.executeController();
			ctrl.deinitController();
			return res.body();
			/*
			String query = req.queryParams("q");
			if( query == null || query.isEmpty() )
			{
				halt(404, "File not found");
				return "";
			}
			else
			{
				res.status(200);
				res.type("text/plain");
				return new String( runQuery(query), StandardCharsets.UTF_8 );
			}*/
		});
	}

	private static void staticRoute(String urlPrefix, String resourceDirectory)
	{
		get(urlPrefix+ "/*", (req, res) -> {
			try( InputStream in = PrimeProject.class.getResourceAsStream( resourceDirectory+ "/" +req.splat()[0] ) )
			{
				//TODO make sure that the URL doesn't include any ".."s
				if( in == null )
					halt(404, "File Not Found");

				res.status(200);
				res.header("Content-Type", Utils.guessMimeType(req.splat()[0]));
				byte[] fileData = Utils.getBytesFromStream(in);
				return new String(fileData, StandardCharsets.UTF_8);
			}
		});
	}

	public static Connection createDBConnection() throws IOException, SQLException
	{
		return DriverManager.getConnection("jdbc:derby:memory:myDB;create=true");
	}

	private static byte[] runQuery(String query)
	{
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(byteOut);

		out.printf("Query: %s\n\n", query);

		try( Connection cnt = createDBConnection();
			Statement stmt = cnt.createStatement() )
		{
			boolean hasResultSet = stmt.execute(query);
			if( hasResultSet )
			{
				ResultSet rs = stmt.getResultSet();

				//pretty-print the result set
				int[] columnWidths = new int[ rs.getMetaData().getColumnCount() ];
				for( int x = 0; x < columnWidths.length; x++ )
					columnWidths[x] = rs.getMetaData().getColumnDisplaySize(x+1);

				//column names
				for( int x = 0; x < columnWidths.length; x++ )
					out.printf("|%" +columnWidths[x]+ "s|", rs.getMetaData().getColumnName(x+1));
				out.println();

				//divider
				for( int x = 0; x < columnWidths.length; x++ )
				{
					out.printf("+");
					for( int y = 0; y < columnWidths[x]; y++ )
						out.printf("-");
					out.printf("+");
				}
				out.println();

				//row data
				while( rs.next() )
				{
					for( int x = 0; x < columnWidths.length; x++ )
						out.printf("|%" +columnWidths[x]+ "s|", rs.getObject(x+1).toString());
					out.println();
				}

				rs.close();
			}
			else
			{
				out.println( "update count = " +stmt.getUpdateCount() );
			}
		}
		catch(Exception e)
		{
			e.printStackTrace(out);
		}

		out.flush();
		return byteOut.toByteArray();
	}
}
