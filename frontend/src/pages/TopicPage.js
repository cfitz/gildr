import React from 'react'                                                                                                          
import Topic from 'features/topic/Topic';


const TopicPage = props => {
  const { topicId } = props.match.params;
  return( <Topic topicId={topicId} /> )
}


export default TopicPage
