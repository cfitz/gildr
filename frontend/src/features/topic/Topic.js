import React, {Component} from "react";                                                                                                         
import { withStyles } from '@material-ui/core/styles';                                                                                          
import { connect } from 'react-redux';       
import {Link} from 'react-router-dom' 

import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import Paper from '@material-ui/core/Paper';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemAvatar from '@material-ui/core/ListItemAvatar';
import Avatar from '@material-ui/core/Avatar';


import {findTopic} from './topicActions';

const styles = theme => ({
  root: {
    width: '100%',
    backgroundColor: theme.palette.background.paper,
  },
  inline: {
    display: 'inline',
  },
});


export class Topic extends Component {

  componentDidMount() {
    const {topicId} = this.props;
    this.props.dispatch(findTopic(topicId));
  }

  render() {
    const {topic, classes} = this.props;
    const {title,author,body} = topic;
    return( 
        <Grid container direction='column' justify='center' alignItems='center' spacing={16}>  
          <Grid item xs={12} > 
            <Typography component="h3"  color="textPrimary">{title}</Typography>
            <Typography variant="subtitle2">By: {author}</Typography>
            <Typography variant="body1">{body}</Typography>
          </Grid> 
        </Grid> 
     )
  }
}


const mapStateToProps = ({topic}, ownProps) => {
  
  return {...ownProps, topic };
}

export default withStyles(styles)(connect(mapStateToProps)(Topic));
