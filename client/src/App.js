import React, { Component } from 'react';
import Game from './components/Game';
import Result from './components/Result';
import mockQuestions from './mockQuestions';
import './App.css';
import {SERVER_URL} from "./config";

class App extends Component {
  constructor(props) {
    super(props);

    this.state = {
      counter: 0,
      questionId: 1,
      question: '',
      answerOptions: [],
      userAnswer: '',
      answersCount: {},
      result: '',

      message: null,
    };

    // hard bind event handlers used in render
    this.handleAnswerSelected = this.handleAnswerSelected.bind(this);
    this.onValueChange = this.onValueChange.bind(this);
  }

  getMessage = e => {
    e.preventDefault();
    fetch(`${SERVER_URL}/api`)
      .then(r => r.text())
      .then(message => this.setState({message: message}))
      .catch(e => console.error(e))
  };

  componentDidMount() {
    this.setState({
      question: mockQuestions[0].question,
      answerOptions: mockQuestions[0].answers
    });
  }

  onValueChange(event) {
    this.setState({
      userAnswer: event.target.value
    });
  }

  handleAnswerSelected(event) {
    this.setUserAnswer(event.currentTarget.value);
    if (this.state.questionId < mockQuestions.length) {
        setTimeout(() => this.setNextQuestion(), 300);
      } else {
        setTimeout(() => this.setResults(this.getResults()), 300);
      }
  }

  setUserAnswer(answer) {
    this.setState((state) => ({
      answersCount: {
        ...state.answersCount,
        [answer]: (state.answersCount[answer] || 0) + 1
      },
      userAnswer: answer
    }));
  }

  setNextQuestion() {
    const counter = this.state.counter + 1;
    const questionId = this.state.questionId + 1;
    this.setState({
      counter: counter,
      questionId: questionId,
      question: mockQuestions[counter].question,
      answerOptions: mockQuestions[counter].answers,
      userAnswer: ''
    });
  }

  getResults() {
    const answersCount = this.state.answersCount;
    const answersCountKeys = Object.keys(answersCount);
    const answersCountValues = answersCountKeys.map((key) => answersCount[key]);
    const maxAnswerCount = Math.max.apply(null, answersCountValues);
    return answersCountKeys.filter((key) => answersCount[key] === maxAnswerCount);
  }

  setResults (result) {
    if (result.length === 1) {
      this.setState({ result: result[0] });
    } else {
      this.setState({ result: 'Undetermined' });
    }
  }

  renderGame() {
    return (
      <Game
        userAnswer={this.state.userAnswer}
        answerOptions={this.state.answerOptions}
        questionId={this.state.questionId}
        question={this.state.question}
        questionTotal={mockQuestions.length}
        onAnswerSelected={this.handleAnswerSelected}
      />
    );
  }

  renderResult() {
    return (
      <Result gameResult={this.state.result} />
    );
  }

  render() {
    return (
      <div className="App">
        <div className="App-header">
          <h2>Trivia</h2>
        </div>
        {this.state.result ? this.renderResult() : this.renderGame()}


        <div>
          <form onSubmit={this.getMessage}>
            <label>Call server: </label>
            <input type="submit" value="Submit" />
          </form>

          <p>
            {
             this.state.message ?
                <strong>{this.state.message}</strong> :
                <span>Edit <code>src/App.js</code> and save to reload.</span>
            }
          </p>
        </div>
          Selected option is : {this.state.userAnswer}
      </div>
    );
  }
}

export default App;
