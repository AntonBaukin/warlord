/*===============================================================+
 |            Application Content Localization                   |
 +===============================================================*/

var Content_EN =
{
	iso: "en",

	datefmt: 'MMMM d, yyyy',

	title: "WARLord Skeleton Sample Application",

	about: {
		text: "<h3>About WARLord Skeleton Project</h3> <p> I've created <a href = 'https://github.com/AntonBaukin/embeddy' target = '_blank'>Embeddy</a> OSGi application skeleton that packs in standalone JAR file OSGi application and all it's bundles, integrates Spring Framework 4 MVC with OSGi HTTP Service provided by embedded Jetty server, and applies Nashorn JavaScript engine for the server-side scripting on the base of ZeT JS library — JsX facility. </p> <p> <a href = 'https://github.com/AntonBaukin/warlord' target = '_blank'>WARLord</a> skeleton is much simpler as it's ordinary WAR application to run in standard Java servlet container, such as Apache Tomcat, or in an application server. It includes the same core components as Embeddy does. </p> <p> This sample web application is plain in nature, but the implementation has no brute simplifications. There are Employees working in Departments, related N-to-1, and 1-to-1: department has a head that is one of it's employees. Data are stored in single table in document-oriented form. Each object has one or more projections that are merged by web controllers to represent object as a whole document. Web UI is a single page application on Angular JS framework with my favourite Anger module. Spring MVC connects the web with the backend. Data access is with plain SQL — I've retired from ORM to documents having related projections. </p> <p class = 'footer right'> Thanks for Your attention, Baukin Anton.<br> <a href = 'mailto:%22Anton%20Baukin%22%3canton.baukin@gmail.com'>anton.baukin@gmail.com</a> </p>"
	},

	menu: {
		warlord: "<span>WAR</span><span>Lord</span><span>Skeleton</span>",
		deps: "Departments",
		emps: "Employees"
	},

	deps: {
		add: "Add department",
		name: "Department name",
		nameplace: "Set department name that is required…",
		office: "Office",
		phone: "Phone",
		head: "Head",
		since: "Since",
		close: "Close",
		viewemps: "View employees",
		selhead: "Select head",
		commit: "Commit"
	},

	emps: {
		add: "Add employee",
		first: "First name",
		firstplace: "Set required first name…",
		middle: "Middle name",
		last: "Last name",
		lastplace: "Set required last name…",
		dob: "Date of birth",
		sex: "Sex",
		male: "Male",
		female: "Female",
		pmail: "Personal email",
		pphone: "Personal phone",
		home: "Home address",
		dep: "Works in department",
		depsince: "Since",
		wmail: "Work email",
		wphone: "Work phone",
		close: "Close",
		viewdep: "View department",
		seldep: "Select department",
		commit: "Commit"
	}
}