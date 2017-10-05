/*===============================================================+
 |                      Application Script                       |
 +===============================================================*/

var $MODULES = [ 'ngSanitize', 'anger' ]

ZeT.scope(angular.module('main', $MODULES), function(main)
{
	var anger = angular.module('anger')

	//~: tune visual parameters
	$(document).ready(function()
	{
		//~: measure — (em) width
		var sp = $('<span>—</span>').css({
			padding    : '0',
			margin     : '0',
			fontWeight : 'normal',
			fontSize   : '10px',
			display    : 'inline-block',
			visibility : 'hidden'
		})

		//~: append, measure, remove
		$(document.body).append(sp)
		var m = sp.outerWidth()
		sp.remove()

		//~: log the visual properties
		ZeT.log('viewport width = ', $(window).width(),
		  'px, height = ', $(window).height(), 'px',
		  '; font 10px — is ', m, 'px')
	})

	/**
	 * Global data map contains objects mapped by UUIDs
	 * loaded from the server and left in the memory.
	 *
	 * If reload flag is not set, and the data are
	 * available, the callback is invoked immediately,
	 * and the data are updated in the background.
	 *
	 * If instead of callback function an array is given,
	 * the objects are replaced without the server call.
	 */
	var globalData = {}, globalDataMap = {}
	function loadData(/* [reload], url, f || a */)
	{
		var a = arguments, r = a[0], u = a[1], f = a[2]
		if(!ZeT.isb(r)) { f = u; u = r; r = false }
		ZeT.assert(!ZeT.ises(u) && (ZeT.isf(f) || ZeT.isox(f)))

		function replaceData(data)
		{
			ZeT.assert(ZeT.isox(data))

			//~: previous version
			var x = globalData[u], ids = (x)?{}:(null)

			//~: assign the new
			globalData[u] = data

			//~: map the ids
			ZeT.each(data, function(o) {
				if(ids) ids[o.uuid] = true
				globalDataMap[o.uuid] = o
			})

			//~: remove obsolete mappings
			ZeT.each(x, function(o) {
				if(o.uuid && !ids[o.uuid])
					delete globalDataMap[o.uuid]
			})
		}

		if(ZeT.isox(f)) //?: {has direct data}
			return replaceData(f)

		//?: {has no data for immediate answer}
		if(r === true && globalData[u])
			f.call(this, globalData[u])
		else
		{
			delete globalData[u] //<-- delete obsolete data
			r = false //<-- mark as no call
		}

		//~: (always) load the requested data
		AppData.get(u, function(obj)
		{
			//?: {is jQuery XHR wrapper}
			if(ZeT.isf(obj.statusCode))
				return f.call(this) //<-- threat as error

			//?: {is plain data object}
			ZeT.assert(ZeT.iso(obj))

			//~: replaced the cached data
			replaceData(obj)

			//?: {call the functor now}
			if(!r) f.call(this, obj)
		})
	}

	//~: root controller
	main.controller('root', function($scope, $element, $timeout, $sanitize)
	{
		ZeT.extend($scope, {

			ZeT      : ZeT,
			ZeTS     : ZeTS,

			//~: localization content (page texts)
			Content  : Content_EN,

			//~: secures html tags in text string
			sanitize : $sanitize
		})

		/**
		 * Safe apply this scope: skips the apply
		 * call on $apply and $digest phases.
		 */
		$scope.safeApply = function()
		{
			var scope = this

			//c: check up for safety
			while(scope)
			{
				if(ZeT.in(scope.$$phase, '$apply', '$digest'))
					return

				scope = scope.$parent
			}

			//~: we are safe to apply
			this.$apply()
		}

		//~: reacts on sub-page load
		ZeT.scope(function()
		{
			var loaded = {}

			$scope.pageLoaded = function(id, i, n)
			{
				ZeT.assert(!ZeT.ises(id) && i > 0 && i <= n)
				ZeT.log('Loaded page [', i, ' of ', n, ']: ', id)

				//~: check the page loaded
				ZeT.assert(ZeT.isu(loaded[i]))
				loaded[i] = id

				//?: {not all pages loaded}
				for(var j = 1;(j <= n);j++)
					if(!loaded[j]) return

				//!: trigger the index loaded
				$scope.$broadcast('index-is-ready')
			}
		})

		//~: anchored content support
		ZeT.scope(function()
		{
			function pageHash()
			{
				var page = window.location.hash

				if(ZeT.ises(page) || ZeTS.first(page) != '#')
					return 'main'

				//?: {hash-bang mode}
				if(ZeTS.starts(page, '#!#'))
					return page.substring(3)

				return page.substring(1)
			}

			//~: select and display the initial page
			$scope.$on('index-is-ready', function()
			{
				ZeT.log('Index page items are fully loaded…')

				ZeT.timeout(100, function()
				{
					$scope.$broadcast('pulsing')

					ZeT.timeout(400, function(){
						$scope.$broadcast('content-' + pageHash())
					})

					ZeT.timeout(600, function(){
						$scope.$broadcast('pulse')
					})
				})
			})

			//~: filter scope click broadcasts
			$scope.$root.agFilterBroadcast = function(item)
			{
				if(!ZeTS.starts(item, 'content-'))
					return

				var hash = item.substring('content-'.length)
				window.location.hash = ('hide' == hash)?(''):(hash)
			}
		})

		//~: window resize
		$(window).on('resize', onDocSize)
		$scope.$on('index-is-ready', onDocSize)

		//~: on document size
		function onDocSize()
		{
			var t = ZeT.get($scope, 'win', 'target')
			var w = { width: $(window).width() }

			if(w.width <= 480)
				w.target = 'phone'
			else if(w.width >= 900)
				w.target = 'desktop'
			else
				w.target = 'tablet'

			if(t != w.target)
			{
				w[w.target] = true
				$scope.win = w

				if(!ZeT.ises(t))
					$scope.$broadcast('win-retarget')

				ZeT.log('Targeting window layout: ', $scope.win.target)
				$scope.$broadcast('win-' + $scope.win.target)

				$timeout(function(){})
			}
		}

		//~: set the document title
		$(document).ready(function(){
			$(document).find('head > title').text($scope.Content.title)
		})

		//~: fixes node width to the current
		$scope.fixWidth = function(n, delay)
		{
			ZeT.assert(ZeT.isn(delay) && delay >= 0)
			ZeT.timeout(delay, function(){
				n.css('width', ZeTS.cat(n.outerWidth(), 'px')).
				  addClass('fixed')
			})
		}

		//~: makes the node height patch the reference
		$scope.matchHeight = function(n, ref)
		{
			ZeT.assertn(n).height(ZeT.assertn(ref).height())
		}

		/**
		 * Initializes date-time picker bootstrap component
		 * for the given input field that is followed with
		 * input-group-addon > button to click.
		 */
		$scope.initDatePicker = function() {
			ZeT.timeout(100, $scope.initDatePickerNow, $scope, arguments)
		}

		$scope.initDatePickerNow = function(input, hidden)
		{
			ZeT.assert(input && input[0])
			ZeT.assert(hidden && hidden[0])
			ZeT.assert(hidden.is('input'))

			/**
			 * Takes timestamp in arbitrary time zone and
			 * makes it UTC date via clearing zone shift.
			 */
			function utc(m)
			{
				return moment.utc([ (m = moment(m)).get('year'),
				  m.get('month'),  m.get('date'), 0, 0, 0, 0 ])
			}

			var dp = hidden.datetimepicker({
				showClear   : true,
				format      : 'YYYY-MM-DDTHH:mm:ss.SSSZ',
				keepOpen    : true,
				defaultDate : utc(ZeT.ises(hidden.val())?moment():(hidden.val())),

				icons       : {
					time     : 'fa fa-clock-o',
					date     : 'fa fa-calendar',
					up       : 'fa fa-arrow-up',
					down     : 'fa fa-arrow-down',
					previous : 'fa fa-angle-double-left',
					next     : 'fa fa-angle-double-right',
					today    : 'fa fa-calendar-check-o',
					clear    : 'fa fa-eraser',
					close    : 'fa fa-times-circle'
				}
			}).data('DateTimePicker')

			//!: hide picker on scope destroy
			this.$on('$destroy', function(){ dp.hide() })

			//~: toggle calendar on the following button
			var btn = input.next('.input-group-addon').find('.btn')
			if(btn.length == 1) btn.click(function(){ dp.toggle() })
			input.click(function(){ dp.toggle() })

			function absolutize(node)
			{
				var o = node.offset()
				var h = node.outerHeight()

				$(document.body).append(node.detach().css({
					position: 'absolute',
					top: o.top + 'px',
					left: o.left + 'px',
					height: h + 'px'
				}))
			}

			//~: add date-only class on widget show
			hidden.on('dp.show', function(e)
			{
				var widget = hidden.parent().find('.bootstrap-datetimepicker-widget')
				ZeT.assert(widget.length == 1)
				widget.addClass('date-only').data('DateTimePicker', dp)
				dp.showTime = new Date().getTime()

				//!: this hacks orders the picker to place properly
				$(window).trigger('resize')
				absolutize(widget)
			})

			//~: update model via change event
			hidden.on('dp.change', function(e)
			{
				//~: trigger hidden field to update bound model
				hidden.val((!e.date)?(''):utc(e.date).toDate().toISOString()).
				  trigger('change') //<-- updates the model
			})
		}

		//~: click on body to hide date-time pickers
		$(document.body).click(function(e)
		{
			//?: {clicked in a picker}
			if($(e.target).closest('.bootstrap-datetimepicker-widget').length)
				return

			$('.bootstrap-datetimepicker-widget').each(function()
			{
				var dp = $(this).data('DateTimePicker')

				//?: {not just shown}
				if(dp && dp.showTime + 500 < new Date().getTime())
					dp.hide()
			})
		})

		//~: is view object differs
		$scope.isViewDiffers = function(o)
		{
			var v = ZeT.get(this, 'view', 'obj')
			if(ZeT.isx(v) || ZeT.isx(o))
				return false
			return ZeT.deep
		}
	})

	/**
	 * Default content view routines.
	 */
	function setupDefaults($scope, $element, opts)
	{
		opts = opts || {}
		$scope.view = {}

		//~: find object node by data uuid
		$scope.findObject = function(o, require)
		{
			if(ZeT.isox(o)) o = o.uuid
			ZeT.asserts(o)

			var n = $element.find('[uuid="' + o + '"]')
			ZeT.assert(n.length <= 1 && (!require || n.length == 1))

			return n
		}

		//~: go to element by uuid
		$scope.gotoObject = function(o)
		{
			if(ZeT.isox(o)) o = o.uuid
			ZeT.asserts(o)

			var n = $scope.findObject(o)

			//?: {not found} delay, maybe loading...
			if(!n.length) return $scope.gotoDelayed = o
			delete $scope.gotoDelayed

			//~: expand the object block
			$scope.view.obj = ZeT.deepClone(ZeT.assertn(globalDataMap[o]))
			$scope.safeApply()

			//~: scroll to this element
			var w = n.closest('.content-wrapper')
			w.parent().scrollTop(n.offset().top - w.offset().top - 20)
		}

		//~: go to element if delayed
		$scope.gotoIfDelayed = function(){
			if(!ZeT.isx($scope.gotoDelayed))
				$scope.gotoObject($scope.gotoDelayed)
		}

		//~: listen on go to signal
		if(!ZeT.ises(opts.gotosignal))
			$scope.$on(opts.gotosignal, function(e, o){
				$scope.gotoObject(o)
			})

		/**
		 * Updates global data object and the value
		 * stored in object-like collection.
		 */
		$scope.updateDataObj = function(url, c, obj)
		{
			ZeT.assert(ZeT.isox(c) && ZeT.isox(obj))
			ZeT.assert(!ZeT.ises(url) && !ZeT.ises(obj.uuid))

			//~: update in the collection
			c[obj.uuid] = obj

			//~: update in the global map
			globalDataMap[obj.uuid] = obj

			//~: update in the global data
			ZeT.assertn(globalData[url])[obj.uuid] = obj

			$scope.safeApply() //<-- update the scope
		}
	}

	//~: departments controller
	main.controller('depsCtrl', function($scope, $element)
	{
		setupDefaults($scope, $element, {
			gotosignal: 'goto-department'
		})

		//~: load the data
		$scope.$on('content-departments', $scope.initScope = function()
		{
			loadData('/get/departments', function(deps)
			{
				loadData('/get/employees', function()
				{
					$scope.deps = deps
					$scope.safeApply()
					$scope.gotoIfDelayed()
				})
			})
		})

		//~: person name
		$scope.personName = function(p)
		{
			return ZeTS.catsep(' ', p.firstName, p.middleName, p.lastName)
		}

		//~: get head employee full name
		$scope.headName = function(d)
		{
			//~: employee uuid
			var e = ZeT.get(d, 'head', 'employee')
			if(ZeT.ises(e)) return

			//~: name of loaded employee object
			return $scope.personName(ZeT.assertn(globalDataMap[e]))
		}

		//~: get department employees
		$scope.listDepEmps = function(d)
		{
			if($scope.view.emps != d.uuid || $scope.view.obj.uuid != d.uuid) return
			return ZeT.collect(globalDataMap, function(o){
				if(d.uuid == ZeT.get(o, 'employee', 'department')) return o
			})
		}

		//~: go to (view) employee expanded
		$scope.gotoEmp = function(e)
		{
			$scope.$root.$broadcast('content-hide')
			$scope.$root.$broadcast('content-employees')

			ZeT.timeout(100, function() {
				$scope.$root.$broadcast('goto-employee', e)
			})
		}

		//~: submit edited department
		$scope.submitDep = function()
		{
			var d = ZeT.assertn($scope.view.obj)
			var n = $scope.findObject(d)

			//?: {the department name is not set}
			if(ZeT.ises(d.name))
				return n.find('.form-group.name').addClass('error-empty')

			AppData.post('/update/department', d, function(obj) {
				$scope.updateDataObj('/get/departments', $scope.deps, obj)
			})
		}
	})

	//~: employees controller
	main.controller('empsCtrl', function($scope, $element)
	{
		setupDefaults($scope, $element, {
			gotosignal: 'goto-employee'
		})

		//~: load the data
		$scope.$on('content-departments', $scope.initScope = function()
		{
			loadData('/get/departments', function(deps)
			{
				loadData('/get/employees', function(emps)
				{
					$scope.deps = deps
					$scope.emps = emps
					$scope.safeApply()
				})
			})
		})

		//~: person name
		$scope.personName = function(p)
		{
			return ZeTS.catsep(' ', p.firstName, p.middleName, p.lastName)
		}

		//~: department name
		$scope.depName = function(e)
		{
			var d = ZeT.get(e, 'employee', 'department')
			if(ZeT.ises(d)) return

			//~: get department object
			d = ZeT.assertn(globalDataMap[d])
			return ZeT.asserts(d.name)
		}

		//~: go to (view) department expanded
		$scope.gotoDep = function(e)
		{
			var d = ZeT.get(e, 'employee', 'department')
			if(ZeT.ises(d)) return

			$scope.$root.$broadcast('content-hide')
			$scope.$root.$broadcast('content-departments')

			ZeT.timeout(100, function() {
				$scope.$root.$broadcast('goto-department', d)
			})
		}

		//~: submit edited employee
		$scope.submitEmp = function()
		{
			var e = ZeT.assertn($scope.view.obj)
			var n = $scope.findObject(e)

			//?: {person last name is not set}
			if(ZeT.ises(e.lastName))
				return n.find('.form-group.last-name').addClass('error-empty')

			//?: {person first name is not set}
			if(ZeT.ises(e.firstName))
				return n.find('.form-group.first-name').addClass('error-empty')

			AppData.post('/update/employee', e, function(obj) {
				$scope.updateDataObj('/get/employees', $scope.emps, obj)
			})
		}

		//~: list all the departments
		$scope.listAllDeps = function(e) {
			return ($scope.view.seldep == e.uuid)?($scope.deps):(null)
		}
	})
})