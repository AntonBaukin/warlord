/*===============================================================+
 |                  Application Data Proxy Data                  |
 |                                   / anton.baukin@gmail.com /  |
 +===============================================================*/

var AppData = ZeT.singleInstance('App:Data:Proxy',
{
	STANDALONE       : false,

	get              : function(url, ps, f)
	{
		if(arguments.length == 2)
			{ f = ps; ps = undefined }

		//?: {parameters is uuid}
		if(ZeT.iss(ps)) ps = { uuid: ps }

		//?: {tries global server path}
		if(ZeTS.starts(url, '/'))
			url = url.substring(1)

		ZeT.assert(!ZeT.ises(url) && ZeT.isf(f))
		ZeT.assert(ZeT.isx(ps) || ZeT.isox(ps))

		//!: issue get request
		var get = jQuery.get({ url: url, data: ps })

		get.fail(function(xhr, statusText)
		{
			ZeT.log('AppData.get(', url, ') error [',
			  xhr.status, ']: ', statusText)

			f.call(xhr)
		})

		get.done(function(o, statusText, xhr)
		{
			//?: {not 200 status code}
			ZeT.assert(200 == xhr.status)

			//!: always expect json object
			ZeT.assert(ZeT.isox(o))

			f.call(xhr, o)
		})
	},

	post             : function(url, obj, f)
	{
		ZeT.assert(!ZeT.ises(url) && ZeT.isox(obj) && ZeT.isf(f))

		//?: {tries global server path}
		if(ZeTS.starts(url, '/'))
			url = url.substring(1)

		var x = { //<-- the request
			url         : url,
			type        : 'POST',
			data        : ZeT.o2s(obj), //<-- encode the post payload
			contentType : 'application/json;charset=utf-8'
		}

		//!: issue post request
		var post = jQuery.post(x)

		post.fail(function(xhr, statusText)
		{
			ZeT.log('AppData.post(', url, ') error [',
			  xhr.status, ']: ', statusText)

			f.call(xhr)
		})

		post.done(function(o, statusText, xhr)
		{
			//?: {not 200 status code}
			ZeT.assert(200 == xhr.status)

			//!: always expect json object
			ZeT.assert(ZeT.isox(o))

			f.call(xhr, o)
		})
	}
})