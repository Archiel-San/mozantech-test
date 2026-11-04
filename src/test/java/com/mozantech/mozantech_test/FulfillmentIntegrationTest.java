package com.mozantech.mozantech_test;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FulfillmentIntegrationTest extends IntegrationTestBase {

    // ---------------------------------------------------------------- allocation rules

    @Test
    void orderWithoutStockStaysPending() {
        JsonNode order = newOrder(5);

        assertThat(order.get("fulfilledQuantity").asLong()).isZero();
        assertThat(order.get("completionPercent").asDouble()).isZero();
        assertThat(order.get("completed").asBoolean()).isFalse();
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void insufficientStockGivesPartialCompletion() {
        long orderId = newOrder(5).get("id").asLong();

        newMovement(3);

        JsonNode order = order(orderId);
        assertThat(order.get("fulfilledQuantity").asLong()).isEqualTo(3);
        assertThat(order.get("completionPercent").asDouble()).isEqualTo(60.0);
        assertThat(order.get("completed").asBoolean()).isFalse();
    }

    @Test
    void orderCreatedAfterStockUsesExistingStock() {
        newMovement(10);

        JsonNode order = newOrder(4);

        assertThat(order.get("completed").asBoolean()).isTrue();
        assertThat(sumMovements(true)).isEqualTo(4);
        assertThat(sumMovements(false)).isEqualTo(6);
    }

    @Test
    void movementLargerThanNeededIsSplitAndStaysTraceable() {
        long orderId = newOrder(4).get("id").asLong();

        JsonNode posted = newMovement(10);   // the original keeps the 6 units nobody needed

        assertThat(posted.get("quantity").asInt()).isEqualTo(6);
        assertThat(posted.get("orderId").isNull()).isTrue();
        assertThat(order(orderId).get("completed").asBoolean()).isTrue();

        // order -> movements
        JsonNode used = get("/orders/" + orderId + "/stock-movements");
        assertThat(used).hasSize(1);
        assertThat(used.get(0).get("quantity").asInt()).isEqualTo(4);
        // the piece that went to the order points back to the movement it was split from
        assertThat(used.get(0).get("originId").asLong()).isEqualTo(posted.get("id").asLong());

        // movement -> order
        JsonNode piece = get("/stock-movements/" + used.get(0).get("id").asLong());
        assertThat(piece.get("orderId").asLong()).isEqualTo(orderId);
    }

    @Test
    void oldestPendingOrderIsServedFirst() {
        long first = newOrder(3).get("id").asLong();
        long second = newOrder(3).get("id").asLong();

        newMovement(4);

        assertThat(order(first).get("completed").asBoolean()).isTrue();
        assertThat(order(second).get("fulfilledQuantity").asLong()).isEqualTo(1);
    }

    @Test
    void creationDateIsFilledIn() {
        assertThat(newOrder(1).get("creationDate").isNull()).isFalse();
        assertThat(newMovement(1).get("creationDate").isNull()).isFalse();
    }

    // ---------------------------------------------------------------- update / delete rules

    @Test
    void deletingAnOrderReleasesItsStockToPendingOrders() {
        long first = newOrder(5).get("id").asLong();
        newMovement(5);                               // goes to the first order
        long second = newOrder(5).get("id").asLong(); // nothing left for it
        assertThat(order(second).get("fulfilledQuantity").asLong()).isZero();

        rest.delete("/orders/" + first);

        assertThat(order(second).get("completed").asBoolean()).isTrue();
    }

    @Test
    void movementAlreadyUsedByAnOrderCannotBeChanged() {
        newOrder(5);
        long movementId = newMovement(5).get("id").asLong();

        ResponseEntity<JsonNode> response = put("/stock-movements/" + movementId, body("quantity", 9));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void completedOrderCannotBeChanged() {
        long orderId = newOrder(2).get("id").asLong();
        newMovement(2);

        ResponseEntity<JsonNode> response = put("/orders/" + orderId, body("quantity", 5));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---------------------------------------------------------------- validation

    @Test
    void invalidQuantityIsRejected() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/orders",
                body("itemId", itemId, "userId", userId, "quantity", 0), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownItemIsNotFound() {
        ResponseEntity<JsonNode> response = rest.postForEntity("/orders",
                body("itemId", 999, "userId", userId, "quantity", 1), JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------------------------------------------------------------- email

    @Test
    void completedOrderSendsExactlyOneEmailToItsOwner() {
        newOrder(2);
        newMovement(1);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));   // still incomplete

        newMovement(1);                                                   // completes it

        ArgumentCaptor<SimpleMailMessage> email = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(email.capture());
        assertThat(email.getValue().getTo()).containsExactly("ana@test.com");
    }

    @Test
    void emailFailureDoesNotUndoTheFulfilment() {
        doThrow(new MailSendException("smtp is down")).when(mailSender).send(any(SimpleMailMessage.class));
        long orderId = newOrder(2).get("id").asLong();

        newMovement(2);   // must still answer 2xx even though the email fails

        assertThat(order(orderId).get("completed").asBoolean()).isTrue();
    }

    // ---------------------------------------------------------------- concurrency

    @Test
    void concurrentOrdersNeverSpendTheSameStockTwice() throws Exception {
        newMovement(10);

        runConcurrently(20, () -> newOrder(1));   // 20 orders of 1 unit, only 10 units exist

        long completed = 0;
        for (JsonNode order : get("/orders")) {
            if (order.get("completed").asBoolean()) {
                completed++;
            }
        }
        assertThat(completed).isEqualTo(10);
        assertThat(sumMovements(true)).isEqualTo(10);
        assertThat(sumMovements(false)).isZero();
    }

    @Test
    void concurrentMovementsAreEachUsedOnlyOnce() throws Exception {
        for (int i = 0; i < 10; i++) {
            newOrder(1);
        }

        runConcurrently(10, () -> newMovement(1));

        for (JsonNode order : get("/orders")) {
            assertThat(order.get("completed").asBoolean()).isTrue();
        }
        assertThat(sumMovements(true)).isEqualTo(10);
        assertThat(sumMovements(false)).isZero();
    }

    private void runConcurrently(int threads, java.util.concurrent.Callable<JsonNode> action) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<JsonNode>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();       // release all threads at the same moment
                return action.call();
            }));
        }
        start.countDown();
        for (Future<JsonNode> future : futures) {
            future.get(60, TimeUnit.SECONDS);
        }
        pool.shutdown();
    }
}