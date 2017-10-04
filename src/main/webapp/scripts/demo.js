/*===============================================================+
 |                Application Proxy with Demo Data               |
 |                                   / anton.baukin@gmail.com /  |
 +===============================================================*/

ZeT.extend(AppData,
{
	STANDALONE       : true,

	get              : function(url, ps, f)
	{
		if(arguments.length == 2)
			{ f = ps; ps = undefined }

		//?: {parameters is uuid}
		if(ZeT.iss(ps)) ps = { uuid: ps }

		ZeT.assert(!ZeT.ises(url) && ZeT.isf(f))
		ZeT.assert(ZeT.isx(ps) || ZeT.isox(ps))

		//~: generate-get the data
		var data = this.get$demo(url, ps)

		//~: invoke the callback asynchronously
		ZeT.timeout(10, function()
		{
			if(ZeT.isx(data))
				f.call({ status: 404 })
			else
				f.call({ status: 200 }, data)
		})
	},

	get$demo         : function(url, ps)
	{
		ZeT.log('Get demo data: ', url, ps)

		if(url == '/get/departments')
			return ZeT.deepClone(this.get$deps())

		if(url == '/get/employees')
			return ZeT.deepClone(this.get$emps())
	},

	get$deps         : function()
	{
		if(this.data$deps)
			return this.data$deps

		function office()
		{
			var a = this.$a('123456789', 1)
			var b = this.$a('0123456789', this.$n(2))
			var c = this.$bool(4)?this.$a('ABCDEF', 1):''
			return ZeTS.cat(a, b, c)
		}

		var self = this, deps = this.data$deps = []

		//~: generate departments
		this.$times(5, function()
		{
			deps.push({
				uuid   : self.uuid(),
				name   : self.$cap(self.$words(5)),
				phone  : self.$phone(),
				office : office.call(self)
			})
		})

		//~: generate employees
		var emps = this.get$emps()

		//~: select head & set department
		ZeT.each(emps, function(e)
		{
			var d = self.$a(deps, 1)[0]

			//=: work in this department
			e.employee.department = d.uuid

			//?: {make it head}
			if(!d.head || self.$bool(3))
				d.head = {
					employee : e.uuid,
					since    : self.$date(-365).toISOString()
				}
		})

		return this.data$deps
	},

	get$emps         : function()
	{
		if(this.data$emps)
			return this.data$emps

		var ADDRESSES = ZeT.s2o(this.ADDRESSES)

		function address()
		{
			var a = this.$a(ADDRESSES, 1)[0]
			return ZeTS.catsep(', ', ZeTS.catsep(' ', a.building, a.street),
			  ZeTS.catsep(' ', a.settlement, a.province), a.index
			)
		}

		var self = this, emps = this.data$emps = []

		//~: generate employees
		this.$times(35, function()
		{
			var e; emps.push(e = {
				uuid       : self.uuid(),
				lastName   : self.$cap(self.$words(1)),
				firstName  : self.$cap(self.$words(1)),
				phone      : self.$phone(),
				email      : self.$email(),
				dob        : self.$date(-365 * 50).toISOString(),
				address    : address.call(self),

				employee   : {
				  phone    : self.$phone(),
				  email    : self.$email(),
				  hired    : self.$date(-365 * 2).toISOString()
				}
			})

			if(self.$bool(3))
				e.middleName = self.$cap(self.$words(1))

			if(!self.$bool(5))
				e.sex = self.$bool()?'M':'F'
		})

		return this.data$emps
	},

	/**
	 * Creates temporary client-side UUID-like object.
	 * Server replaces it with own values.
	 */
	uuid             : ZeT.scope(function()
	{
		//~: template
		var XY = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'

		//~: timestamp
		var ts = new Date().getTime()
		try{ ts += performance.now() }catch(ignore){}

		//~: generator
		function xy(c)
		{
			var r = (ts + Math.random()*16)%16 | 0
			return ((c == 'x')?(r):(r&0x3|0x8)).toString(16)
		}

		return function() {
			return XY.replace(/[xy]/g, xy)
		}
	}),

	$n               : function(m, M)
	{
		if(arguments.length == 1) { M = m; m = 0 }
		return m + Math.floor(Math.random() * (1 + M - m))
	},

	$times           : function(/* m, [ M, ] f */)
	{
		var m, M, f, a = arguments

		if(a.length == 3) { m = a[0]; M = a[1]; f = a[2] }
		else { m = 1; M = a[0]; f = a[1] }

		var n = this.$n(m, M)
		for(var i = 0;(i < n);i++) f()
	},

	/**
	 * Returns n unique randomly selected items of the array-like.
	 */
	$au              : function(a, n)
	{
		ZeT.assert(ZeT.isi(a.length) && a.length > 0)
		ZeT.assert(ZeT.isi(n) && n > 0)
		if(a.length < n) n = a.length

		var r = [], ii = {}

		function is(i)
		{
			if(ii[i]) return false

			ii[i] = true
			r.push(a[i])

			return true
		}

		main: while(r.length < n)
		{
			var i = Math.floor(Math.random() * a.length)

			if(is(i)) continue

			for(var j = i + 1;(j < a.length);j++)
				if(is(j)) continue main

			for(j = 0;(j < i);j++)
				if(is(j)) continue main

			throw ZeT.ass()
		}

		return r
	},

	/**
	 * Returns n randomly selected items of the array-like.
	 */
	$a               : function(a, n)
	{
		ZeT.assert(ZeT.isi(a.length) && a.length > 0)
		ZeT.assert(ZeT.isi(n) && n >= 0)

		for(var r = [], i = 0;(i < n);i++)
			r.push(a[Math.floor(Math.random() *a.length)])

		return r
	},

	$words           : function(up, from)
	{
		return this.$au(this.WORDS, this.$n(1, up)).join(' ')
	},

	$phone           : function()
	{
		var D = '0123456789'
		return ZeTS.cat('+1-', this.$a(D, 3), '-', this.$a(D, 7))
	},

	$email           : function()
	{
		return ZeTS.cat(
		  ZeTS.catsep(this.$bool()?'.':'_', this.$words(2).split(' ')),
		  '@', ZeTS.catsep('.', this.$words(2).split(' '), 'com')
		)
	},

	$cap             : function(s)
	{
		return ZeTS.first(s).toUpperCase() + s.substring(1)
	},

	$bool            : function(n)
	{
		return Math.random() < (1 / ((n || 1) + 1))
	},

	/**
	 * Creates date shifted [0, n) days from the
	 * given point, or default this day.
	 */
	$date            : function(n, at)
	{
		var d = ZeT.isu(at)?(new Date()):
		  ZeT.isn(at)?(new Date(at)):(new Date(at.getTime()))

		d.setUTCHours(0, 0, 0, 0)
		n = AppData.$n(1, n || 7)
		d.setDate(d.getDate() + n)

		return d
	},

	WORDS            : ['the','name','very','through','and','just','form','much','great','think','you','say','that','help','low','was','line','for','before','turn','are','cause','with','same','mean','differ','his','move','they','right','boy','old','one','too','have','does','this','tell','from','sentence','set','had','three','want','hot','air','but','well','some','also','what','play','there','small','end','can','put','out','home','other','read','were','hand','all','port','your','large','when','spell','add','use','even','word','land','how','here','said','must','big','each','high','she','such','which','follow','act','their','why','time','ask','men','will','change','way','went','about','light','many','kind','then','off','them','need','would','house','write','picture','like','try','these','again','her','animal','long','point','make','mother','thing','world','see','near','him','build','two','self','has','earth','look','father','more','head','day','stand','could','own','page','come','should','did','country','found','sound','answer','school','most','grow','number','study','who','still','over','learn','know','plant','water','cover','than','food','call','sun','first','four','people','thought','may','let','down','keep','side','eye','been','never','now','last','find','door','any','between','new','city','work','tree','part','cross','take','since','get','hard','place','start','made','might','live','story','where','saw','after','far','back','sea','little','draw','only','left','round','late','man','run','year','came','while','show','press','every','close','good','night','real','give','life','our','few','under','stop','Rank','Word','Rank','Word','open','ten','seem','simple','together','several','next','vowel','white','toward','children','war','begin','lay','got','against','walk','pattern','example','slow','ease','center','paper','love','often','person','always','money','music','serve','those','appear','both','road','mark','map','book','science','letter','rule','until','govern','mile','pull','river','cold','car','notice','feet','voice','care','fall','second','power','group','town','carry','fine','took','certain','rain','fly','eat','unit','room','lead','friend','cry','began','dark','idea','machine','fish','note','mountain','wait','north','plan','once','figure','base','star','hear','box','horse','noun','cut','field','sure','rest','watch','correct','color','able','face','pound','wood','done','main','beauty','enough','drive','plain','stood','girl','contain','usual','front','young','teach','ready','week','above','final','ever','gave','red','green','list','though','quick','feel','develop','talk','sleep','bird','warm','soon','free','body','minute','dog','strong','family','special','direct','mind','pose','behind','leave','clear','song','tail','measure','produce','state','fact','product','street','black','inch','short','lot','numeral','nothing','class','course','wind','stay','question','wheel','happen','full','complete','force','ship','blue','area','object','half','decide','rock','surface','order','deep','fire','moon','south','island','problem','foot','piece','yet','told','busy','knew','test','pass','record','farm','boat','top','common','whole','gold','king','possible','size','plane','heard','age','best','dry','hour','wonder','better','laugh','true','thousand','during','ago','hundred','ran','check','remember','game','step','shape','early','yes','hold','hot','west','miss','ground','brought','interest','heat','reach','snow','fast','bed','five','bring','sing','sit','listen','perhaps','six','fill','table','east','travel','weight','less','language','morning','among'],

	ADDRESSES        : "[{\"building\":\"9730\",\"street\":\"Arcadia St.\",\"settlement\":\"North Canton\",\"province\":\"OH\",\"index\":\"44720\"},{\"building\":\"227\",\"street\":\"Tailwater Dr.\",\"settlement\":\"Catonsville\",\"province\":\"MD\",\"index\":\"21228\"},{\"building\":\"665\",\"street\":\"West Fairground St.\",\"settlement\":\"Bangor\",\"province\":\"ME\",\"index\":\"04401\"},{\"building\":\"385\",\"street\":\"Valley View Court\",\"settlement\":\"Howell\",\"province\":\"NJ\",\"index\":\"07731\"},{\"building\":\"5\",\"street\":\"Shub Farm St.\",\"settlement\":\"Springfield\",\"province\":\"PA\",\"index\":\"19064\"},{\"building\":\"997\",\"street\":\"Van Dyke Avenue\",\"settlement\":\"Linden\",\"province\":\"NJ\",\"index\":\"07036\"},{\"building\":\"4\",\"street\":\"Penn St.\",\"settlement\":\"Somerset\",\"province\":\"NJ\",\"index\":\"08873\"},{\"building\":\"7131\",\"street\":\"Halifax Dr.\",\"settlement\":\"Jamaica Plain\",\"province\":\"MA\",\"index\":\"02130\"},{\"building\":\"9718\",\"street\":\"West St.\",\"settlement\":\"Hyattsville\",\"province\":\"MD\",\"index\":\"20782\"},{\"building\":\"1\",\"street\":\"Squaw Creek Ave.\",\"settlement\":\"Chelsea\",\"province\":\"MA\",\"index\":\"02150\"},{\"building\":\"3\",\"street\":\"S. Pacific Street\",\"settlement\":\"Cranston\",\"province\":\"RI\",\"index\":\"02920\"},{\"building\":\"99\",\"street\":\"Sherwood Street\",\"settlement\":\"Owosso\",\"province\":\"MI\",\"index\":\"48867\"},{\"building\":\"94\",\"street\":\"Warren Ave.\",\"settlement\":\"East Orange\",\"province\":\"NJ\",\"index\":\"07017\"},{\"building\":\"33\",\"street\":\"Augusta Circle\",\"settlement\":\"Ada\",\"province\":\"OK\",\"index\":\"74820\"},{\"building\":\"7188\",\"street\":\"North Grandrose St.\",\"settlement\":\"Sun City\",\"province\":\"AZ\",\"index\":\"85351\"},{\"building\":\"260\",\"street\":\"Evergreen Ave.\",\"settlement\":\"Shelbyville\",\"province\":\"TN\",\"index\":\"37160\"},{\"building\":\"6\",\"street\":\"Vernon Street\",\"settlement\":\"Neptune\",\"province\":\"NJ\",\"index\":\"07753\"},{\"building\":\"170\",\"street\":\"Pendergast St.\",\"settlement\":\"Granger\",\"province\":\"IN\",\"index\":\"46530\"},{\"building\":\"8293\",\"street\":\"Amherst St.\",\"settlement\":\"Natchez\",\"province\":\"MS\",\"index\":\"39120\"},{\"building\":\"60\",\"street\":\"W. Brook Street\",\"settlement\":\"Camas\",\"province\":\"WA\",\"index\":\"98607\"},{\"building\":\"5\",\"street\":\"Center St.\",\"settlement\":\"Dalton\",\"province\":\"GA\",\"index\":\"30721\"},{\"building\":\"137\",\"street\":\"Bridgeton St.\",\"settlement\":\"Aiken\",\"province\":\"SC\",\"index\":\"29803\"},{\"building\":\"2\",\"street\":\"Thatcher Drive\",\"settlement\":\"Bellmore\",\"province\":\"NY\",\"index\":\"11710\"},{\"building\":\"7668\",\"street\":\"North Bridge St.\",\"settlement\":\"Gettysburg\",\"province\":\"PA\",\"index\":\"17325\"},{\"building\":\"400\",\"street\":\"South Vale Street\",\"settlement\":\"Santa Clara\",\"province\":\"CA\",\"index\":\"95050\"},{\"building\":\"97\",\"street\":\"West Pendergast Street\",\"settlement\":\"Hialeah\",\"province\":\"FL\",\"index\":\"33010\"},{\"building\":\"704\",\"street\":\"Gates Road\",\"settlement\":\"New Port Richey\",\"province\":\"FL\",\"index\":\"34653\"},{\"building\":\"74\",\"street\":\"Marconi Street\",\"settlement\":\"Naples\",\"province\":\"FL\",\"index\":\"34116\"},{\"building\":\"8490\",\"street\":\"South Whitemarsh Street\",\"settlement\":\"Maumee\",\"province\":\"OH\",\"index\":\"43537\"},{\"building\":\"352\",\"street\":\"Ashley Lane\",\"settlement\":\"Opa Locka\",\"province\":\"FL\",\"index\":\"33054\"},{\"building\":\"352\",\"street\":\"Glendale St.\",\"settlement\":\"Chelsea\",\"province\":\"MA\",\"index\":\"02150\"},{\"building\":\"634\",\"street\":\"West Cypress Drive\",\"settlement\":\"Atlantic City\",\"province\":\"NJ\",\"index\":\"08401\"},{\"building\":\"280\",\"street\":\"Leatherwood Street\",\"settlement\":\"Nazareth\",\"province\":\"PA\",\"index\":\"18064\"},{\"building\":\"30\",\"street\":\"Mill Street\",\"settlement\":\"Valdosta\",\"province\":\"GA\",\"index\":\"31601\"},{\"building\":\"240\",\"street\":\"Tailwater Circle\",\"settlement\":\"Titusville\",\"province\":\"FL\",\"index\":\"32780\"},{\"building\":\"92\",\"street\":\"Wood Street\",\"settlement\":\"Gainesville\",\"province\":\"VA\",\"index\":\"20155\"},{\"building\":\"8552\",\"street\":\"Shirley St.\",\"settlement\":\"Monroe\",\"province\":\"NY\",\"index\":\"10950\"},{\"building\":\"857\",\"street\":\"Hill Dr.\",\"settlement\":\"Depew\",\"province\":\"NY\",\"index\":\"14043\"},{\"building\":\"9301\",\"street\":\"NE. Branch Ave.\",\"settlement\":\"Muskogee\",\"province\":\"OK\",\"index\":\"74403\"},{\"building\":\"618\",\"street\":\"Mayflower Ave.\",\"settlement\":\"Noblesville\",\"province\":\"IN\",\"index\":\"46060\"},{\"building\":\"8713\",\"street\":\"Thompson Street\",\"settlement\":\"Mount Prospect\",\"province\":\"IL\",\"index\":\"60056\"},{\"building\":\"8\",\"street\":\"Trenton Dr.\",\"settlement\":\"Gurnee\",\"province\":\"IL\",\"index\":\"60031\"},{\"building\":\"91\",\"street\":\"Howard Drive\",\"settlement\":\"Lebanon\",\"province\":\"PA\",\"index\":\"17042\"},{\"building\":\"9965\",\"street\":\"North Edgemont Street\",\"settlement\":\"Harleysville\",\"province\":\"PA\",\"index\":\"19438\"},{\"building\":\"355\",\"street\":\"Walt Whitman Court\",\"settlement\":\"Greenfield\",\"province\":\"IN\",\"index\":\"46140\"},{\"building\":\"100\",\"street\":\"Marshall Street\",\"settlement\":\"Milford\",\"province\":\"MA\",\"index\":\"01757\"},{\"building\":\"7507\",\"street\":\"Golden Star Street\",\"settlement\":\"Encino\",\"province\":\"CA\",\"index\":\"91316\"},{\"building\":\"8392\",\"street\":\"Jackson Avenue\",\"settlement\":\"Phillipsburg\",\"province\":\"NJ\",\"index\":\"08865\"},{\"building\":\"2\",\"street\":\"Gulf Rd.\",\"settlement\":\"Victoria\",\"province\":\"TX\",\"index\":\"77904\"},{\"building\":\"613\",\"street\":\"Oakland Street\",\"settlement\":\"Westland\",\"province\":\"MI\",\"index\":\"48185\"},{\"building\":\"8779\",\"street\":\"Edgewood Circle\",\"settlement\":\"Asheville\",\"province\":\"NC\",\"index\":\"28803\"},{\"building\":\"673\",\"street\":\"Lake View Street\",\"settlement\":\"Hamtramck\",\"province\":\"MI\",\"index\":\"48212\"},{\"building\":\"8769\",\"street\":\"Water Street\",\"settlement\":\"Apple Valley\",\"province\":\"CA\",\"index\":\"92307\"},{\"building\":\"8941\",\"street\":\"Walnut Street\",\"settlement\":\"Hopkins\",\"province\":\"MN\",\"index\":\"55343\"},{\"building\":\"8075\",\"street\":\"Saxon Drive\",\"settlement\":\"Henderson\",\"province\":\"KY\",\"index\":\"42420\"},{\"building\":\"8011\",\"street\":\"Grandrose Dr.\",\"settlement\":\"Stuart\",\"province\":\"FL\",\"index\":\"34997\"},{\"building\":\"6\",\"street\":\"Union Court\",\"settlement\":\"Lake Villa\",\"province\":\"IL\",\"index\":\"60046\"},{\"building\":\"63\",\"street\":\"E. Olive Lane\",\"settlement\":\"Urbandale\",\"province\":\"IA\",\"index\":\"50322\"},{\"building\":\"9877\",\"street\":\"W. Court Lane\",\"settlement\":\"Woonsocket\",\"province\":\"RI\",\"index\":\"02895\"},{\"building\":\"65\",\"street\":\"San Carlos Ave.\",\"settlement\":\"Saint Cloud\",\"province\":\"MN\",\"index\":\"56301\"},{\"building\":\"876\",\"street\":\"Wood Drive\",\"settlement\":\"Oakland Gardens\",\"province\":\"NY\",\"index\":\"11364\"},{\"building\":\"28\",\"street\":\"S. Brook Court\",\"settlement\":\"Vineland\",\"province\":\"NJ\",\"index\":\"08360\"},{\"building\":\"7\",\"street\":\"North Market Ave.\",\"settlement\":\"Kingsport\",\"province\":\"TN\",\"index\":\"37660\"},{\"building\":\"67\",\"street\":\"East Ave.\",\"settlement\":\"Perth Amboy\",\"province\":\"NJ\",\"index\":\"08861\"},{\"building\":\"9019\",\"street\":\"North Newcastle Ave.\",\"settlement\":\"Asbury Park\",\"province\":\"NJ\",\"index\":\"07712\"},{\"building\":\"9841\",\"street\":\"Stillwater St.\",\"settlement\":\"Grand Rapids\",\"province\":\"MI\",\"index\":\"49503\"},{\"building\":\"9213\",\"street\":\"Jefferson St.\",\"settlement\":\"Peachtree City\",\"province\":\"GA\",\"index\":\"30269\"},{\"building\":\"49\",\"street\":\"Edgewater Drive\",\"settlement\":\"Logansport\",\"province\":\"IN\",\"index\":\"46947\"},{\"building\":\"7755\",\"street\":\"Lake Forest St.\",\"settlement\":\"Santa Clara\",\"province\":\"CA\",\"index\":\"95050\"},{\"building\":\"329\",\"street\":\"Old York Lane\",\"settlement\":\"Sykesville\",\"province\":\"MD\",\"index\":\"21784\"},{\"building\":\"175\",\"street\":\"Walnut Ave.\",\"settlement\":\"Apple Valley\",\"province\":\"CA\",\"index\":\"92307\"},{\"building\":\"9066\",\"street\":\"Indian Spring Drive\",\"settlement\":\"Bethpage\",\"province\":\"NY\",\"index\":\"11714\"},{\"building\":\"92\",\"street\":\"Newcastle Dr.\",\"settlement\":\"Toms River\",\"province\":\"NJ\",\"index\":\"08753\"},{\"building\":\"81\",\"street\":\"Pine Street\",\"settlement\":\"Fort Washington\",\"province\":\"MD\",\"index\":\"20744\"},{\"building\":\"611\",\"street\":\"Tanglewood Street\",\"settlement\":\"Maineville\",\"province\":\"OH\",\"index\":\"45039\"},{\"building\":\"215\",\"street\":\"E. Hill Field St.\",\"settlement\":\"Fort Lee\",\"province\":\"NJ\",\"index\":\"07024\"},{\"building\":\"55\",\"street\":\"Fairfield Court\",\"settlement\":\"Covington\",\"province\":\"GA\",\"index\":\"30014\"},{\"building\":\"68\",\"street\":\"Mayfair Ave.\",\"settlement\":\"Fredericksburg\",\"province\":\"VA\",\"index\":\"22405\"},{\"building\":\"8775\",\"street\":\"Sussex Ave.\",\"settlement\":\"North Kingstown\",\"province\":\"RI\",\"index\":\"02852\"},{\"building\":\"687\",\"street\":\"Rockaway St.\",\"settlement\":\"Coatesville\",\"province\":\"PA\",\"index\":\"19320\"},{\"building\":\"1\",\"street\":\"Court Street\",\"settlement\":\"Atwater\",\"province\":\"CA\",\"index\":\"95301\"},{\"building\":\"7929\",\"street\":\"Santa Clara Rd.\",\"settlement\":\"Clarkston\",\"province\":\"MI\",\"index\":\"48348\"},{\"building\":\"8335\",\"street\":\"South 2nd St.\",\"settlement\":\"Johnson City\",\"province\":\"TN\",\"index\":\"37601\"},{\"building\":\"8793\",\"street\":\"Roehampton Court\",\"settlement\":\"San Pablo\",\"province\":\"CA\",\"index\":\"94806\"},{\"building\":\"8005\",\"street\":\"San Pablo Dr.\",\"settlement\":\"Palm Bay\",\"province\":\"FL\",\"index\":\"32907\"},{\"building\":\"830\",\"street\":\"South Carriage St.\",\"settlement\":\"Suitland\",\"province\":\"MD\",\"index\":\"20746\"},{\"building\":\"904\",\"street\":\"Bear Hill Rd.\",\"settlement\":\"Saint Charles\",\"province\":\"IL\",\"index\":\"60174\"},{\"building\":\"43\",\"street\":\"Wilson Rd.\",\"settlement\":\"Fort Wayne\",\"province\":\"IN\",\"index\":\"46804\"},{\"building\":\"26\",\"street\":\"South Plumb Branch Drive\",\"settlement\":\"Baltimore\",\"province\":\"MD\",\"index\":\"21206\"},{\"building\":\"59\",\"street\":\"Sunbeam Rd.\",\"settlement\":\"Mount Pleasant\",\"province\":\"SC\",\"index\":\"29464\"},{\"building\":\"597\",\"street\":\"Sugar Rd.\",\"settlement\":\"Valparaiso\",\"province\":\"IN\",\"index\":\"46383\"},{\"building\":\"606\",\"street\":\"Argyle Road\",\"settlement\":\"Hartford\",\"province\":\"CT\",\"index\":\"06106\"},{\"building\":\"330\",\"street\":\"Thatcher Drive\",\"settlement\":\"Westminster\",\"province\":\"MD\",\"index\":\"21157\"},{\"building\":\"96\",\"street\":\"East Central Dr.\",\"settlement\":\"Tampa\",\"province\":\"FL\",\"index\":\"33604\"},{\"building\":\"37\",\"street\":\"Fawn Drive\",\"settlement\":\"Roswell\",\"province\":\"GA\",\"index\":\"30075\"},{\"building\":\"9614\",\"street\":\"W. Poplar Drive\",\"settlement\":\"Amsterdam\",\"province\":\"NY\",\"index\":\"12010\"},{\"building\":\"154\",\"street\":\"North Overlook Ave.\",\"settlement\":\"Mahopac\",\"province\":\"NY\",\"index\":\"10541\"},{\"building\":\"641\",\"street\":\"Overlook St.\",\"settlement\":\"Rapid City\",\"province\":\"SD\",\"index\":\"57701\"},{\"building\":\"65\",\"street\":\"Wall Drive\",\"settlement\":\"Riverdale\",\"province\":\"GA\",\"index\":\"30274\"},{\"building\":\"213\",\"street\":\"Mayfair St.\",\"settlement\":\"Hartselle\",\"province\":\"AL\",\"index\":\"35640\"}]"
})