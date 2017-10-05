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
		ZeT.assert(!ZeT.ises(u) && (ZeT.isf(f) || ZeT.isa(f)))

		function replaceData(data)
		{
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

		if(ZeT.isa(f)) //?: {has direct data}
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
			if(ZeT.iso(obj)) //?: {is error}
				return f.call(this)

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
		$scope.initDatePicker = function(input, hidden)
		{
			ZeT.assert(input && input[0])
			ZeT.assert(hidden && hidden[0])
			ZeT.assert(hidden.is('input'))

			var dp = hidden.datetimepicker({
				showClear   : true,
				format      : 'YYYY-MM-DDTHH:mm:ssZ',
				keepOpen    : true,

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
				var v = (!e.date)?(''):moment(e.date).format()

				//if(!e.date) dp.hide(); else
				//	//?: {not only the time had changed}
				//	if(e.oldDate && !e.date.isSame(e.oldDate, 'day'))
				//		dp.hide()

				ZeT.log('Value ', v)

				//hidden.val(v).trigger('change')
			})
		}

		//~: click on body to hide date-time picker
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
	})

	/**
	 * Default content view routines.
	 */
	function setupDefaults($scope, $element, opts)
	{
		opts = opts || {}
		$scope.view = {}

		//~: go to element bu uuid
		$scope.gotoObject = function(o)
		{
			if(ZeT.isox(o)) o = o.uuid
			ZeT.asserts(o)

			var n = $element.find('[uuid="' + o + '"]')
			ZeT.assert(n.length <= 1)

			//?: {not found} delay, maybe loading...
			if(!n.length) return $scope.gotoDelayed = o
			delete $scope.gotoDelayed

			//~: expand the object block
			$scope.view.obj = o
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
					$scope.filtered = deps
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
			if($scope.view.emps != d.uuid || $scope.view.obj != d.uuid) return
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
			loadData('/get/departments', function()
			{
				loadData('/get/employees', function(emps)
				{
					$scope.filtered = emps
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
	})
})
