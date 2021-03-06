package pp;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import lib.persistence.entities.Item;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public abstract class HTMLController extends Controller
{
	private final Set<String> externalCSS = new HashSet<>();
	private final Set<String> externalJS = new HashSet<>();
	private final List<View> output = new ArrayList<>();
	private final Map<String, Object> controllerContext = new HashMap<>();
	private final PageGenerationTimer timer = new PageGenerationTimer();

	private class PageGenerationTimer
	{
		private final long start = System.nanoTime();
		public double secondsToCreatePage()
		{
			long nsTime = System.nanoTime() - start;
			double secTime = nsTime / (double)TimeUnit.SECONDS.toNanos(1);
			return secTime;
		}
	}

	//helper functions
	protected final void addCSS(String cssURL)
	{
		externalCSS.add( cssURL );
	}

	protected final void addJS(String jsURL)
	{
		externalJS.add( jsURL );
	}

	protected final void addWebFont(String webfontURL)
	{
		externalCSS.add( webfontURL );
	}

	protected final void bindData(String key, Object value)
	{
		controllerContext.put( key, value );
	}

	protected final void println(String line)
	{
		output.add( new TextView(line+ "\n") );
	}

	protected final HandlebarsView outputView(String viewResource) throws IOException
	{
		final HandlebarsView v = new HandlebarsView(viewResource);
		output.add( v );
		return v;
	}

	private interface View
	{
		String get() throws Exception;
	}

	private class TextView implements View
	{
		private final String text;

		private TextView(String text)
		{
			this.text = text;
		}

		public String get()
		{
			return text;
		}
	}

	protected class HandlebarsView implements View
	{
		private final String viewResource;
		private final Map<String, Object> viewContext = new HashMap<>();

		private HandlebarsView(String viewResource)
		{
			this.viewResource = viewResource;
		}

		public HandlebarsView bindData(String key, Object value)
		{
			viewContext.put( key, value );
			return this;
		}

		public String get() throws IOException
		{
			//get the template
			String templateText = Utils.getFileText( PrimeProject.class.getResourceAsStream( viewResource ) );

			//create the context
			Context ctx = Context
				.newBuilder( HTMLController.this.timer )
				.combine( HTMLController.this.controllerContext )
				.combine( viewContext )
				.resolver(MapValueResolver.INSTANCE, MethodValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
				.build();

			//compile and apply the template
			final Handlebars hbs = new Handlebars();
			Template template = hbs.compileInline( templateText );
			return template.apply( ctx );
		}
	}

	//helper objects
	protected class ControllerItem
	{
		public final long id;
		public final String name, desc, cost;

		public ControllerItem(Item item)
		{
			this.id = item.getId();
			this.name = item.getName();
			this.desc = item.getDescription();
			this.cost = String.format("$%d.%2d", item.getCostDollar(), item.getCostCents());
		}
	}

	//lifecycle functions
	public final void executeController() throws Exception
	{
		bindData("isUserLoggedIn", isUserLoggedIn());
		bindData("username", bl().getUsername());

		bindData("url", req().pathInfo());

		bindData("cartCount", bl().getCartCount());
		bindData("cartTotal", bl().getCartTotal());

		addJS("/static/jquery.js");
		addJS("/static/primes.js");

		addCSS("/static/main.css");
		addCSS("/static/header.css");
		addCSS("/static/footer.css");
		addCSS("/static/panel.css");
		addCSS("/static/item.css");

		addWebFont("https://fonts.googleapis.com/css?family=Lato");

		generatePage();
		finalizePageGeneration();
	}

	protected abstract void generatePage() throws Exception;

	private void finalizePageGeneration() throws Exception
	{
		String page = "";
		page += "<!DOCTYPE html>\n";
		page += "<html>\n";
		page += "<head>\n";

		for( String css : externalCSS )
			page += String.format("<link href='%s' type='text/css' rel='stylesheet' />\n", css);

		for( String js : externalJS )
			page += String.format("<script src='%s'></script>\n", js);

		page += "</head>\n";
		page += "<body>\n";

		for( View v : output )
			page += v.get();

		page += "</body>\n";
		page += "</html>\n";

		res().status(200);
		res().header("Content-Type", "text/html;charset=utf-8");
		res().body( page );
	}
}
