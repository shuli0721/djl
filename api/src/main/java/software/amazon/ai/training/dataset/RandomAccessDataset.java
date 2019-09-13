/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package software.amazon.ai.training.dataset;

import java.util.RandomAccess;
import java.util.concurrent.ExecutorService;
import software.amazon.ai.Device;
import software.amazon.ai.training.Trainer;
import software.amazon.ai.util.Pair;

/**
 * RandomAccessDataset represent the dataset that support random access reads. i.e. it could access
 * certain data item given the index
 */
public abstract class RandomAccessDataset<I, L> implements Dataset<I, L>, RandomAccess {

    protected long size;
    protected Sampler sampler;
    protected ExecutorService executor;
    protected int prefetchNumber;
    protected Device device;

    public RandomAccessDataset(BaseBuilder<?> builder) {
        this.sampler = builder.getSampler();
        this.executor = builder.getExecutor();
        this.prefetchNumber = builder.getPrefetchNumber();
        this.device = builder.getDevice();
    }

    public abstract Pair<I, L> get(long index);

    @Override
    public Iterable<Batch> getData(Trainer<I, L, ?> trainer) {
        return new DataIterable<>(this, trainer, sampler, executor, prefetchNumber, device);
    }

    public long size() {
        return size;
    }

    @SuppressWarnings("rawtypes")
    public abstract static class BaseBuilder<T extends BaseBuilder> {

        private Sampler sampler;
        private ExecutorService executor;
        private int prefetchNumber;
        private Device device;

        public Sampler getSampler() {
            if (sampler == null) {
                throw new IllegalArgumentException("The sampler must be set");
            }
            return sampler;
        }

        public T setSampling(long batchSize) {
            return setSampling(batchSize, true, false);
        }

        public T setSampling(long batchSize, boolean shuffle) {
            return setSampling(batchSize, shuffle, false);
        }

        public T setSampling(long batchSize, boolean shuffle, boolean dropLast) {
            Sampler.SubSampler subSampler = shuffle ? new RandomSampler() : new SequenceSampler();
            sampler = new BatchSampler(subSampler, batchSize, dropLast);
            return self();
        }

        public T setSampler(Sampler sampler) {
            this.sampler = sampler;
            return self();
        }

        public ExecutorService getExecutor() {
            return executor;
        }

        public int getPrefetchNumber() {
            return prefetchNumber;
        }

        public T optExcutor(ExecutorService executor, int prefetchNumber) {
            this.executor = executor;
            this.prefetchNumber = prefetchNumber;
            return self();
        }

        public Device getDevice() {
            return device;
        }

        public T optDevice(Device device) {
            this.device = device;
            return self();
        }

        protected abstract T self();
    }
}