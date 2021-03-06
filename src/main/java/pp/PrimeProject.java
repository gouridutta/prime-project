package pp;

import lib.imagedb.ImageDB;
import lib.imagedb.PersistentImageDB;
import lib.persistence.DataAccessObject;
import lib.persistence.dao.SQLiteDAO;
import lib.persistence.entities.Item;
import pp.controllers.*;
import spark.Request;
import spark.Response;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import static spark.Spark.*;

public class PrimeProject
{
	private static final boolean DEBUG = true; //set this to FALSE in non-development builds
	private static SessionManager sm;
	private static ImageDB imageDB;

	public static void main(String[] args)
	{
		port(8080);

		sm = new SessionManager();
		imageDB = new PersistentImageDB();

		if( DEBUG )
		{
			try
			{
				DataAccessObject dao = new SQLiteDAO();
				createRandomItems(dao);
			}
			catch( Exception e )
			{
				throw new RuntimeException("Could not create random items", e);
			}
		}

		before("/debug/*", (req, res) -> {
			if( !DEBUG )
				halt(404, "File Not Found");
		});

		get("/", (req, res) -> useController( new HomepageController(), req, res ) );

		get("/cart", (req, res) -> useController( new CartController(), req, res ) );

		get("/item/:id", (req, res) -> useController( new ItemController(), req, res ) );

		post("/item/:id/performCreateReview", (req, res) -> useController(new PerformCreateReviewController(), req, res) );

		get("/itemImage/:id", (req, res) -> useController( new ItemImageController(), req, res ) );

		get("/login", (req, res) -> useController( new LoginController(), req, res ) );

		get("/createAccount", (req, res) -> useController( new CreateAcctController(), req, res ) );

		get("/search", (req, res) -> useController( new SearchController(), req, res ) );

		post("/performAcctCreate", (req, res) -> useController( new PerformCreateAcctController(), req, res) );

		post("/performLogin", (req, res) -> useController( new PerformLoginController(), req, res ) );

		get("/performLogout", (req, res) -> useController( new PerformLogoutController(), req, res ) );

		get("/api/listCartItems", (req, res) -> useController( new CartAPIController.ListCartAPIController(), req, res ) );

		get("/api/addCartItem", (req, res) -> useController( new CartAPIController.AddToCartAPIController(), req, res ) );

		get("/api/updateCartQuantity", (req, res) -> useController(new CartAPIController.UpdateCartQuantityAPIController(), req, res ) );

		staticRoute("/static", "/www/static");

		get("/debug", (req, res) -> useController( new DebugController.DashboardController(), req, res ) );
		get("/debug/db/account", (req, res) -> useController( new DebugController.AccountDBViewer(), req, res ) );
		get("/debug/db/item", (req, res) -> useController( new DebugController.ItemDBViewer(), req, res ) );

		after((req, res) -> {
			//does the user support gzip?
			if( req.headers("Accept-Encoding") != null && req.headers("Accept-Encoding").contains("gzip") )
				res.header("Content-Encoding", "gzip");
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
				return Utils.getFileText( in );
			}
		});
	}

	private static String useController(final Controller ctrl, final Request req, final Response res) throws Exception
	{
		ctrl.initController(req, res, new SQLiteDAO(), imageDB, sm);
		ctrl.executeController();
		ctrl.deinitController();
		return res.body();
	}

	private static void createRandomItems(DataAccessObject dao) throws Exception
	{
		//Only generate items for the database if the database is empty
		if( !dao.itemAccessor().allItems().isEmpty() )
			return;

		final String[] names = {
				"Earring",
				"Spoon",
				"Cup of Noodles",
				"Red Solo Cup",
				"Lamp",
				"Computer Monitor",
				"Watter Bottle",
				"Empty Snapple Bottle",
				"Coffee Maker",
				"Lint Roller"
		};

		final BufferedImage img = ImageIO.read( PrimeProject.class.getResourceAsStream("/item_images/earrings.jpg") );

		for( String name : names )
		{
			final Item i = new Item(name, "This is a " + name, (int) (Math.random() * 100), (int) (Math.random() * 99));
			dao.itemAccessor().create(i);
			imageDB.put( i.getId(), img );
		}
	}
}
