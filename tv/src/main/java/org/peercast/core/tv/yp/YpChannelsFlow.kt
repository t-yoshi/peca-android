package org.peercast.core.tv.yp

import kotlinx.coroutines.flow.MutableStateFlow
import org.peercast.core.lib.rpc.YpChannel

class YpChannelsFlow : MutableStateFlow<List<YpChannel>> by MutableStateFlow(emptyList())