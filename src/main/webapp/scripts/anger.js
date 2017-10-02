/*===============================================================+
 |                Event Driven Angular Directives                |
 |                                   / anton.baukin@gmail.com /  |
 +===============================================================*/

ZeT.scope(angular.module('anger', []), function(anger)
{
	/**
	 * Anger directives prefix.
	 */
	var ANGER = 'ag'

	/**
	 * Anger directives prefix utility.
	 */
	function ag(name, attr)
	{
		ZeT.asserts(name)

		if(attr === true)
			return ANGER + '-' + name

		//~: camel case '-'
		var cc = name.split('-')
		for(var i = 0;(i < cc.length);i++)
			cc[i] = cc[i].substring(0, 1).toLocaleUpperCase() +
			  cc[i].substring(1)

		return ANGER + cc.join('')
	}

	var CMD_SYMBOLS = '!?~{'

	function cmdSym(s)
	{
		var x = ZeTS.first(ZeT.asserts(s))
		return ZeT.ii(CMD_SYMBOLS, x)?(x):(undefined)
	}

	function cmdArg(s)
	{
		var x = ZeTS.first(ZeT.asserts(s))
		return !ZeT.ii(CMD_SYMBOLS, x)?(s):(s.substring(1))
	}

	function cmdCall(s, f)
	{
		ZeT.asserts(s)
		ZeT.assertf(f)

		return function()
		{
			var args = ZeT.a(arguments)
			args.unshift(cmdSym(s))
			return f.apply(this, args)
		}
	}

	function parseAttrEvents(events, scope, f)
	{
		var self = this

		ZeT.asserts(events)
		ZeT.assertn(scope)
		ZeT.assertf(f)

		//?: {event is an object}
		if(events.charAt(0) == '{')
		{
			events = scope.$eval(events)
			ZeT.assert(ZeT.isox(events))

			ZeT.each(events, function(o, s)
			{
				ZeTS.each(s, function(xs){
					f.call(self, [ xs, o ])
				})
			})

			return
		}

		//~: iterate over regular string
		ZeTS.each(events, function(s){
			f.call(self, s)
		})
	}

	function eachAttrEvent(an, f)
	{
		ZeT.asserts(an)
		ZeT.assertf(f)

		return function(scope, node, attr)
		{
			var self = this
			var   ax = ZeTS.trim(ZeT.asserts(attr[ag(an)]))
			var args = ZeT.a(arguments)
			args.unshift('')

			parseAttrEvents(ax, scope, function(s)
			{
				args[0] = s
				f.apply(self, args)
			})
		}
	}

	//~: template id
	anger.directive(ag('template-id'), function()
	{
		return {
			restrict   : 'A',

			template   : function($element, $attrs)
			{
				//~: the template id
				var id = ZeT.asserts($attrs[ag('template-id')])

				//~: remove the id attribute
				$element.removeAttr(ag('template-id', true))

				//~: save the html
				$element.data(ag('template-html'), $element[0].outerHTML)

				//~: write id to the node
				$element.attr('id', ag('template-id', true) + '-' + id)

				return ''
			}
		}
	})

	/**
	 * Includes template from node found by the ag-template-id.
	 * Takes the following options in ag-ots:
	 *
	 *  extract: to remove the template tag and adding it's
	 *    content into the target element directly;
	 *
	 *  replace: to replace the target element with the
	 *    template's root, or with the content nodes, if
	 *    this flag is combined with extract one.
	 *
	 *  With these two options, there are four combinations,
	 *  each having a meaning and an application.
	 */
	anger.directive(ag('template'), function()
	{
		return {
			restrict   : 'A',

			template   : function($element, $attrs)
			{
				//~: the template id
				var id = ZeT.asserts($attrs[ag('template')])

				//~: find the template node
				var n = $('#' + ag('template-id', true) + '-' + id).first()

				//?: {the node does not exist}
				ZeT.assert(n.length == 1, 'Node #',
				  ag('template-id', true), '-', id, ' is not found!')

				return ZeT.asserts(n.data(ag('template-html')))
			},

			link       : function($scope, $element, $attrs)
			{
				var o = $opts($element, $attrs, $scope) || {}
				var u = $element.children().first() //<-- inner root
				var f //<-- target action

				//?: {template is not included as a single child}
				ZeT.assert($element.children().length == 1)

				//?: {simple extraction} add the children
				if(o.extract && !o.replace)
				{
					$element.append(u.children())
					u.remove()
				}

				//?: {replace the target element} replace with the target
				else if(!o.extract && o.replace)
					$element.replaceWith(u)

				//?: {replace with extract} replace with the children
				else if(o.extract && o.replace)
					$element.replaceWith(u.children())
			}
		}
	})

	//~: creates isolated scope from the object
	anger.directive(ag('scope'), function()
	{
		return {
			restrict   : 'A',
			scope      : true,

			controller : function($scope, $attrs)
			{
				var s = $attrs[ag('scope')]
				if(ZeT.ises(s)) return
				ZeT.extend($scope, $scope.$eval(s))
			}
		}
	})

	//~: trim (whitespaces between tags)
	anger.directive(ag('trim'), function($timeout)
	{
		function filter()
		{
			return (this.nodeType == 3) && !/\S/.test(this.nodeValue)
		}

		function clear()
		{
			$(this).contents().filter(filter).remove()
			$(this).children().each(clear)
		}

		return function(scope, node)
		{
			clear.apply(node)
		}
	})

	//?: {is scope in apply or digest}
	function isApplyDigest(scope)
	{
		//c: check up for safety
		while(scope)
		{
			if(ZeT.in(scope.$$phase, '$apply', '$digest'))
				return true
			scope = scope.$parent
		}

		return false
	}

	//~: if — adds or deletes content on events
	anger.directive(ag('if'), [ '$animate', '$timeout', function($animate, $timeout)
	{
		return {
			restrict   : 'A',
			transclude : 'element',
			priority   : 600,
			terminal   : true,

			link       : function($scope, $element, $attr, ctrl, $transclude)
			{
				var added

				function add()
				{
					if(!added) $transclude(function(clone, child)
					{
						added = { $scope: child, $element: clone }
						$animate.enter(clone, $element.parent(), $element)
					})
				}

				function del()
				{
					if(added) try
					{
						$animate.leave(added.$element)
					}
					finally
					{
						try
						{
							added.$scope.$destroy()
						}
						finally
						{
							added = null
						}
					}
				}

				function toggle(x)
				{
					var f = (x)?(add):(del)

					//?: {scope is in digest} direct call, else timeout
					if(isApplyDigest($scope)) f(); else $timeout(f)
				}

				ZeT.scope($scope, $element, $attr, eachAttrEvent('if', function(s)
				{
					$scope.$on(cmdArg(s), cmdCall(s, function(x)
					{
						toggle(ZeTS.first(x) != '!')
					}))
				}))
			}
		}
	}])

	//~: focus-on
	anger.directive(ag('focus-on'), function()
	{
		return eachAttrEvent('focus-on', function(s, scope, node, attr)
		{
			scope.$on(cmdArg(s), cmdCall(s, function(x, event)
			{
				if((x == '?') && !ZeTS.ises(node.val()))
					return //<-- skip fields with value

				//?: {default prevented}
				if(event.defaultPrevented)
					return

				//~: prevent the triggering event
				event.preventDefault()

				//!: WARNING: do not apply delay for the focus
				node.focus()
			}))
		})
	})

	/**
	 * Complex toggle class with encoded name.
	 * The class name code consists of the following:
	 *    [C]class-name[+delay][-delay][=duration]
	 *
	 * where
	 *
	 *   [C]          is the command prefix '~' or '!';
	 *   [+delay]     optional delay to add the class;
	 *   [-delay]     optional delay to remove the class;
	 *   [=duration]  tells the time while the class is set,
	 *                the class is removed after it.
	 *
	 * If no delay is given, the action is instant.
	 *
	 * The command prefix tells what to do:
	 *  ~ actually toggles the class if it is set or not;
	 *  ! always removes the class if it's set.
	 *
	 * The default (empty) command is to add the class
	 * if it isn't set yet.
	 */
	function toggleClass(n, c)
	{
		if(ZeT.ises(c)) return

		//~: decode the command
		var x = ZeTS.first(c)
		if(!ZeT.in(x, '~', '!')) x = null; else c = c.substring(1)

		//~: decode the add delay
		var a, m = c.match(/\+\d+/)
		if(m && m[0]) {
			c = ZeTS.replace(c, m[0], '')
			a = parseInt(m[0].substring(1))
		}

		//~: decode the remove delay
		var r; m = c.match(/-\d+/)
		if(m && m[0]) {
			c = ZeTS.replace(c, m[0], '')
			r = parseInt(m[0].substring(1))
		}

		//~: decode the auto-remove duration
		var d; m = c.match(/=\d+/)
		if(m && m[0]) {
			c = ZeTS.replace(c, m[0], '')
			d = parseInt(m[0].substring(1))
		}

		//?: {has the class now}
		var t = true, h = n.hasClass(c)

		//~: treat the commands
		if(x == '~') t = !h
		if(x == '!') t = false

		//?: {state it the same}
		if(t == h) return

		//?: {add class now}
		if(t && !a) {
			if(d) ZeT.timeout(d, function(){ n.removeClass(c) })
			return n.addClass(c)
		}

		//?: {remove class now}
		if(!t && !r) return n.removeClass(c)

		//?: {add with delay}
		if(t) return ZeT.timeout(a, function(){
			if(d) ZeT.timeout(d, function(){ n.removeClass(c) })
			n.addClass(c)
		})

		//~: remove with delay
		return ZeT.timeout(r, function(){ n.removeClass(c) })
	}

	//~: toggles one or more classes
	function toggleClasses(n, cs)
	{
		var f = ZeT.fbind(toggleClass, this, n)

		if(ZeT.iss(cs)) ZeTS.each(cs, f)
		else ZeT.each(cs, f)
	}

	//~: class
	anger.directive(ag('class'), function()
	{
		return eachAttrEvent('class', function(s, scope, node, attr)
		{
			if(!ZeT.isa(s)) return

			var e = ZeT.asserts(s[0])
			var c = s[1]; if(ZeT.iss(c)) c = [ c ]
			ZeT.asserta(c)

			scope.$on(e, function(event)
			{
				ZeT.each(c, function(cls){
					toggleClasses(node, cls)
				})
			})
		})
	})

	//~: init: passes $element and $scope
	anger.directive(ag('init'), function()
	{
		return function($scope, $element, $attrs)
		{
			var o = $opts($element, $attrs, $scope)
			var s = $attrs[ag('init')]

			function f()
			{
				if(!ZeT.ises(s))
					ZeT.xeval(s, {
						$scope   : $scope,
						$element : $element
					})
			}

			if(o && ZeT.isn(o.delay))
				ZeT.timeout(o.delay, f)
			else
				f()
		}
	})

	/**
	 * Invokes script after each Angular $digest cycle
	 * completes. Primary usage is to handle elements
	 * after something had changed. Script can use
	 * $element and $scope variables.
	 */
	anger.directive(ag('digest'), function()
	{
		return function(scope, node, attr)
		{
			var script = attr[ag('digest')]
			if(ZeT.ises(script)) return

			//~: flag of single-shot registration
			var f; scope.$watch(function()
			{
				if(f) return; else f = true
				scope.$$postDigest(function()
				{
					f = false

					ZeT.xeval(script, {
						$scope   : scope,
						$element : node
					})
				})
			})
		}
	})

	//~: on: reacts on listed events
	anger.directive(ag('on'), function()
	{
		return eachAttrEvent('on', function(s, scope, node, attr)
		{
			scope.$on(cmdArg(s), cmdCall(s, function(x, event)
			{
				var script = attr[ag('on-' + s)]

				if(!ZeT.ises(script))
					ZeT.xeval(script, {
						$scope   : scope,
						$element : node
					})
			}))
		})
	})

	//~: access options object or individual option
	function opts(node, p)
	{
		var opts = $(node).data('ag-opts')
		if(!opts) return

		ZeT.assert(ZeT.iso(opts))
		return ZeT.isu(p)?(opts):(opts[p])
	}

	//~: raw function to extract, parse and remember the options
	function $opts(node, attrs, scope)
	{
		var opts = $(node).data('ag-opts')
		if(opts) return opts

		var opts = ZeTS.trim(attrs[ag('opts')])
		if(ZeT.ises(opts)) return

		ZeT.assert(ZeTS.first(opts) == '{')
		opts = (scope)?(scope.$eval(opts)):ZeT.xeval('return '.concat(opts))
		ZeT.assert(ZeT.iso(opts))

		node.data('ag-opts', opts)
		return opts
	}

	//~: provies options object for other Anger directives
	anger.directive(ag('opts'), function()
	{
		return function(scope, node, attrs) {
			return $opts(node, attrs, scope)
		}
	})

	function eachAttrEventHandler(h, an, action, wrapper)
	{
		ZeT.assertf(h)
		ZeT.asserts(an)
		ZeT.assertf(action)

		var eae = eachAttrEvent(an, action)

		return function(/* scope, node, ... */)
		{
			var self = this, args = ZeT.a(arguments)

			h.call(this, args, [an, action, wrapper], function()
			{
				if(!ZeT.isf(wrapper))
					eae.apply(self, args)
				else
				{
					var a = ZeTA.copy(args)

					a.unshift(function() {
						return eae.apply(self, args)
					})

					wrapper.apply(self, a)
				}
			})
		}
	}

	function eachAttrEventClick(an, action, wrapper)
	{
		function h(args, aaw, f)
		{
			var n = $(args[1])
			n.click(protect(n, pulse(n, delay(n, wrap(args, f)))))
		}

		function protect(n, f)
		{
			//~: protection timeout milliseconds
			var a = opts(n, 'active')
			if(ZeT.isx(a)) return f
			ZeT.assert(ZeT.isi(a) && a >= 0)

			//~: is disabled flag
			var disabled = false

			return function(e)
			{
				if(disabled) {
					e.stopImmediatePropagation()
					e.preventDefault()
				}

				//~: temporary disable for repeated click protection
				disabled = true; n.addClass('active')
				ZeT.timeout(a, function(){
					disabled = false; n.removeClass('active')
				})

				return f.apply(this, arguments)
			}
		}

		function pulse(n, f)
		{
			//~: pulse timeout milliseconds
			var p = opts(n, 'pulse')
			if(ZeT.isx(p)) return f
			ZeT.assert(ZeT.isi(p) && p >= 0)

			return function()
			{
				n.addClass('pulse')
				ZeT.timeout(p, function(){
					n.removeClass('pulse')
				})

				return f.apply(this, arguments)
			}
		}

		function delay(n, f)
		{
			//~: delay timeout milliseconds
			var d = opts(n, 'delay')
			if(ZeT.isx(d)) return f
			ZeT.assert(ZeT.isi(d) && d >= 0)
			return function()
			{
				ZeT.timeout(d, f, this, arguments)
			}
		}

		//~: temporary adds $event object
		function wrap(args, f)
		{
			return function(e)
			{
				//~: set jQuery event to the scope
				var s = args[0], x = s.$event
				s.$event = e

				try
				{
					return f.apply(this, arguments)
				}
				finally
				{
					if(x) s.$event = x; else delete s.$event
				}
			}
		}

		function ifclick(eae, scope, node)
		{
			var ifc = opts(node, 'if')
			if(ZeT.isf(ifc)) ifc = ifc.call(scope, node)

			if(ifc !== false)
				if(ZeT.isf(wrapper))
					return wrapper.apply(this, arguments)
				else
					return eae.apply(this, arguments)
		}

		return eachAttrEventHandler(h, an, action, ifclick)
	}

	function scopeUp(scope, up)
	{
		ZeT.assertn(scope)
		ZeT.assert(ZeT.isi(up))

		while(up-- > 0)
			if(!scope.$parent) break; else
				scope = scope.$parent

		return scope
	}

	//~: eachAttrEventClick() callback that fires event broadcast
	function eventBroadcast(s, scope, node)
	{
		var up = opts(node, 'up')
		if(ZeT.isi(up)) scope = scopeUp(scope, up)

		smartBroadcast(scope, s)
	}

	/**
	 * Smart version of event broadcasting.
	 * It supports delayed events having +delay
	 * suffix in the event name.
	 */
	function smartBroadcast(scope, e)
	{
		if(ZeT.isa(e))
			b(e[0], e[1])
		else
			b(e)

		function b(x, data)
		{
			//~: decode the delay suffix of the event
			var d = x.match(/\+(\d)+$/)
			if(d) {
				x = x.substring(0, x.length - d[0].length)
				d = parseInt(d[0])
				ZeT.assert(ZeT.isn(d) & d >= 0)
			}

			function f()
			{
				scope.$broadcast.apply(scope, data?[x, data]:[x])
			}

			if(!d) f(); else ZeT.timeout(d, f)
		}
	}

	//~: click broadcast
	anger.directive(ag('click'), function()
	{
		return eachAttrEventClick('click', eventBroadcast)
	})

	//~: pulse broadcast upon initialization
	anger.directive(ag('pulse'), function()
	{
		return eachAttrEvent('pulse', function(s, $scope){
			smartBroadcast($scope, s)
		})
	})

	//~: key enter broadcast
	anger.directive(ag('key-enter'), function()
	{
		function h(args, aaw, f)
		{
			$(args[1]).keypress(function(e)
			{
				if(e.which == 13) f()
			})
		}

		return eachAttrEventHandler(h, 'key-enter', eventBroadcast)
	})

	/**
	 * Traverses the scopes tree starting with the given
	 * node, then going to all it's descendants.
	 *
	 * Traversing goes by the tree levels the number
	 * as the second argument of the callback.
	 *
	 * When callback returns false, traversing is finished
	 * with the result of that node.
	 */
	function traverse(scope, f)
	{
		var l = 0, level = [ scope ]

		//c: while there are levels
		while(level.length)
		{
			//c: ask for the level accumulated
			for(var i = 0;(i < level.length);i++)
				//?: {stop by current}
				if(false === f(level[i], l))
					return level[i]

			var next = [] //<-- the next level

			//c: accumulate the next level
			for(i = 0;(i < level.length);i++)
			{
				var x = level[i].$$childHead

				while(x)
				{
					next.push(x)
					x = x.$$nextSibling
				}
			}

			//~: swap the level
			level = next; l++
		}
	}

	//~: history handling strategy
	var History = anger.History =
	{
		HID       : '29aa6a6a-3c51-11e6-ac61-9e71128cae77',

		replace   : function(/* fname, scope, obj */)
		{
			window.history.replaceState(
			  History.state.apply(History, arguments), '')
		},

		push      : function(/* fname, scope, obj */)
		{
			var s = History.state.apply(History, arguments)
			var x = window.history.state

			//?: {state is not a duplicate}
			if(ZeT.o2s(x) != ZeT.o2s(s))
				window.history.pushState(s, '')
		},

		$roots    : {},

		state     : function(fname, scope, obj)
		{
			ZeT.asserts(fname)
			ZeT.assertn(scope)

			//?: {the scope given is temporary}
			if(scope.$$transcluded == true)
				return { uuid: History.HID, f: 'noop' }

			//~: the root scope
			var id, rs = ZeT.assertn(scope.$root)

			//~: search for the already saved
			ZeT.each(History.$roots, function(k, s){
				if(s === rs) { id = k; return false }
			})

			if(!id) //?: {root scopes is not saved}
			{
				id = '' + new Date().getTime()
				ZeT.assert(!History.$roots[id])
				History.$roots[id] = rs

				//!: potential memory leak

				//~: watch the scope destroyed
				rs.$on('$destroy', function() {
					delete History.$roots[id]
				})
			}

			return {
				uuid : History.HID,
				f    : fname,
				rid  : id,
				sid  : scope.$id,
				obj  : obj
			}
		},

		call      : function(e)
		{
			//?: {not our state}
			var s = ZeT.get(e, 'originalEvent', 'state')
			if(!ZeT.iso(s) || s.uuid != History.HID) return

			var f = ZeT.assertf(History[s.f])
			f.call(History, s)
		},

		noop      : function(){},

		broadcast : function(s)
		{
			var events = ZeT.asserts(ZeT.get(s, 'obj', 'events'))
			var   node = s.obj.node
			if(ZeT.iss(node)) node = $('#' + node)

			//~: the root scope
			var rs = ZeT.assertn(History.$roots[s.rid],
			  'No root scope $', s.rid, ' is found in History!')

			//~: traverse the root scope
			var scope; traverse(rs, function(x)
			{
				if(x.$id == s.sid) { scope = x; return false }
			})

			//?: {scope is not found}
			ZeT.assertn(scope, 'History referred Angular ',
			  'scope $id = ', s.scope, ' is not found!')

			//~: broadcast the events
			parseAttrEvents(events, scope, function(s){
				eventBroadcast(s, scope, node)
			})
		}
	}

	//~: react on module generated history states
	$(window).on('popstate', History.call)

	function nodeId(node)
	{
		var n = $(node).first()
		ZeT.assert(1 === n.length)

		var id = node.attr('id')
		if(!ZeT.ises(id)) return id

		if(!anger.$nodeId) anger.$nodeId = 1
		node.attr('id', id = (ANGER + '-' + anger.$nodeId++))

		return id
	}

	//~: click push to history
	anger.directive(ag('click-history'), function()
	{
		function wrapper(f, scope, node, attrs)
		{
			History.push('broadcast', scope, {
			  node: nodeId(node),
			  events: attrs[ag('click-history')],
			})

			f()
		}

		return eachAttrEventClick('click-history',
		  eventBroadcast, wrapper)
	})
})