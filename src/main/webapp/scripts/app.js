/*===============================================================+
 |                      Application Script                       |
 +===============================================================*/

var $MODULES = [ 'ngSanitize', 'anger' ]

ZeT.scope(angular.module('main', $MODULES), function(module)
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

	//~: root controller
	module.controller('root', function($scope, $element, $timeout, $sanitize)
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

		//~: fixes node width to the current
		$scope.fixWidth = function(n, delay)
		{
			ZeT.assert(ZeT.isn(delay) && delay >= 0)

			ZeT.timeout(delay, function(){
				n.css('width', ZeTS.cat(n.outerWidth(), 'px')).
				  addClass('fixed')
			})
		}

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
	})
})
