import { FIND_TOPIC, TOPIC_FOUND } from './topicConstants';

export const findTopic = topicId => ({
  type: FIND_TOPIC,
  topicId
});

export const loadTopic = results => ({
  type: TOPIC_FOUND,
  results
});
