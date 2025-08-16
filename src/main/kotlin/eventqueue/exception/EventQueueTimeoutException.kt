package eventqueue.exception

import kotlinx.coroutines.CancellationException

/**
 * Exception thrown when an event in the EventQueue times out.
 */
class EventQueueTimeoutException(message: String) : CancellationException(message)
