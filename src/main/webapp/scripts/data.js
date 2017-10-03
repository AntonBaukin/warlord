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
	}
})