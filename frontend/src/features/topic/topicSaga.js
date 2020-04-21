import { getTopic } from 'clients/ForumClient';
import { FIND_TOPIC, TOPIC_FOUND } from './topicConstants';

import {put, call} from 'redux-saga/effects'

export  function *  submitTopicShow({topicId}) {
  try {
    const payload = yield call(getTopic, topicId);
    yield put({type: TOPIC_FOUND, payload});
  } catch (err) {
    yield put({type: 'TOPIC_ERROR', err })
  }
}
