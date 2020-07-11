import React, { Component } from 'react';
import logo from './logo.svg';
import './App.css';
import {SERVER_URL} from "./config";

class App extends Component {
  state = {
    message: null,
  }

  getMessage = e => {
    e.preventDefault();
    fetch(`${SERVER_URL}/api`)
      .then(r => r.text())
      .then(message => this.setState({message: message}))
      .catch(e => console.error(e))
  };

  render() {
    return (
      <div className="App">
        <header className="App-header">
          <img src={logo} className="App-logo" alt="logo" />

          <form onSubmit={this.getMessage}>
            <label>Call server: </label>
            <input type="submit" value="Submit" />
          </form>

          <p>
            { this.state.message ?
                <strong>{this.state.message}</strong> :
                <span>Edit <code>src/App.js</code> and save to reload.</span>
            }
          </p>
        </header>
      </div>
    );
  }
}

export default App;
