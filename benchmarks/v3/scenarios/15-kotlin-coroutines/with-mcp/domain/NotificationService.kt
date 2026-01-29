package com.notification.domain

/**
 * Primary interface for the notification service.
 * Defines the contract for sending notifications.
 */
interface NotificationService {
    /**
     * Send a notification to a single channel.
     *
     * @param notification The notification to send
     * @return NotificationResult indicating success or failure
     */
    suspend fun send(notification: Notification): NotificationResult

    /**
     * Send a notification request to multiple channels concurrently.
     *
     * @param request The notification request with multiple channels
     * @return MultiChannelResult with results from all channels
     */
    suspend fun sendMultiChannel(request: NotificationRequest): MultiChannelResult

    /**
     * Send a notification with retry logic.
     *
     * @param notification The notification to send
     * @param maxRetries Maximum number of retry attempts
     * @return NotificationResult indicating final success or failure
     */
    suspend fun sendWithRetry(
        notification: Notification,
        maxRetries: Int = 3
    ): NotificationResult
}

/**
 * Interface for notification strategy selection.
 * Implements the Strategy pattern for channel selection.
 */
interface NotificationStrategy {
    /**
     * Select the appropriate channel for a notification.
     *
     * @param request The notification request
     * @return List of channels to use, ordered by priority
     */
    suspend fun selectChannels(request: NotificationRequest): List<NotificationChannel>

    /**
     * Determine if a channel should be used for the given notification.
     *
     * @param notification The notification to evaluate
     * @param channel The channel to evaluate
     * @return true if the channel should be used
     */
    suspend fun shouldUseChannel(
        notification: Notification,
        channel: NotificationChannel
    ): Boolean
}

/**
 * Interface for notification repository.
 * Handles persistence of notifications.
 */
interface NotificationRepository {
    /**
     * Save a notification.
     *
     * @param notification The notification to save
     * @return The saved notification
     */
    suspend fun save(notification: Notification): Notification

    /**
     * Find a notification by ID.
     *
     * @param id The notification ID
     * @return The notification if found, null otherwise
     */
    suspend fun findById(id: String): Notification?

    /**
     * Find notifications by recipient.
     *
     * @param recipient The recipient address
     * @return List of notifications for the recipient
     */
    suspend fun findByRecipient(recipient: String): List<Notification>

    /**
     * Find notifications by status.
     *
     * @param status The status to filter by
     * @return List of notifications with the given status
     */
    suspend fun findByStatus(status: NotificationStatus): List<Notification>

    /**
     * Update a notification.
     *
     * @param notification The notification to update
     * @return The updated notification
     */
    suspend fun update(notification: Notification): Notification
}
