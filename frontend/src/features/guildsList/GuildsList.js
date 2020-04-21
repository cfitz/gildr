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


import {findGuilds} from './guildsListActions';

const styles = theme => ({
  grid: {
    marginTop: theme.spacing.unit * 4
  },
  root: {
    width: '100%',
    backgroundColor: theme.palette.background.paper,
    paddingTop: theme.spacing.unit * 4
  },
  inline: {
    display: 'inline',
  },
});


export class GuildsList extends Component {

  componentDidMount() {
    this.props.dispatch(findGuilds());
  }

  render() {
    const {guilds, classes} = this.props;
    return( 
        <Grid className={classes.grid} container direction='column' justify='center' alignItems='center' spacing={16}>  
          <Grid item xs={12} > 
            <List className={classes.root}>
            { guilds.map( guild => {
                return (
                 <ListItem key={guild.id} alignItems="flex-start" component={Link} to={`/g/${guild.id}`}>
                    <ListItemText primary={guild.name}
                                  secondary={
                                     <React.Fragment>
                                      { guild.description }
                                    </React.Fragment>
                                  } />
                </ListItem>)
              })
            }
            </List>
          </Grid> 
        </Grid> 
     )
  }
}


const mapStateToProps = ({guilds}) => {
  return {guilds};
}

export default withStyles(styles)(connect(mapStateToProps)(GuildsList));
