//To launch the channel search activity:
//
//
//        val channelSearchActivityIntent = Intent(this, ChannelSearchActivity::class.java)
//
//        // Specify to the LaunchActivity that we came from settings so it doesn't automatically
//        // load the channel we're currently on
//        channelSearchActivityIntent.putExtra(getString(R.string.launchedFromSettings), true)
//        channelSearchActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
//        startActivity(channelSearchActivityIntent)